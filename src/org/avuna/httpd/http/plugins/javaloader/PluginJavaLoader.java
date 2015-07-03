/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins.javaloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.CRC32;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.event.Event;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.event.base.EventID;
import org.avuna.httpd.event.base.EventPostInit;
import org.avuna.httpd.event.base.EventPreExit;
import org.avuna.httpd.event.base.EventReload;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.hosts.VHost;
import org.avuna.httpd.hosts.VHostM;
import org.avuna.httpd.http.Resource;
import org.avuna.httpd.http.ResponseGenerator;
import org.avuna.httpd.http.StatusCode;
import org.avuna.httpd.http.event.EventGenerateResponse;
import org.avuna.httpd.http.event.HTTPEventID;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.Plugin;
import org.avuna.httpd.http.plugins.PluginRegistry;
import org.avuna.httpd.http.plugins.base.PluginSecurity;
import org.avuna.httpd.http.plugins.javaloader.lib.DatabaseManager;
import org.avuna.httpd.util.Config;
import org.avuna.httpd.util.ConfigFormat;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.Logger;
import org.avuna.httpd.util.SafeMode;
import org.avuna.httpd.util.unixsocket.CException;

public class PluginJavaLoader extends Plugin {
	
	private Config config = null;
	
	public PluginJavaLoader(String name, PluginRegistry registry) {
		super(name, registry);
		log("Loading JavaLoader Config & Security");
		PluginSecurity sec = ((PluginSecurity) registry.getPatchForClass(PluginSecurity.class));
		boolean sece = sec.pcfg.getNode("enabled").getValue().equals("true");
		if (sece) {
			try {
				secjlcl = new JavaLoaderClassLoader(new URL[] { AvunaHTTPD.fileManager.getPlugin(registry.getPatchForClass(PluginSecurity.class)).toURI().toURL() }, this.getClass().getClassLoader());
			}catch (MalformedURLException e1) {
				e1.printStackTrace();
			}
		}
		config = new Config(name, new File(getDirectory(), "config.cfg"), new ConfigFormat() {
			public void format(ConfigNode map) {
				
			}
		});
		try {
			config.load();
		}catch (IOException e1) {
			Logger.logError(e1);
		}
		if (sece) ((PluginSecurity) registry.getPatchForClass(PluginSecurity.class)).loadBases(this);
		log("Loading JavaLoader Libs");
		try {
			lib = new File(AvunaHTTPD.fileManager.getMainDir(), pcfg.getNode("lib").getValue());
			if (!lib.exists() || !lib.isDirectory()) {
				lib.mkdirs();
			}
			URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
			Class<URLClassLoader> sysclass = URLClassLoader.class;
			
			try {
				Method method = sysclass.getDeclaredMethod("addURL", URL.class);
				method.setAccessible(true);
				method.invoke(sysloader, new Object[] { lib.toURI().toURL() });
				for (File f : lib.listFiles()) {
					if (!f.isDirectory() && f.getName().endsWith(".jar")) {
						method.invoke(sysloader, new Object[] { f.toURI().toURL() });
					}
				}
			}catch (Throwable t) {
				Logger.logError(t);
			}
			HostHTTP host2 = (HostHTTP) registry.host;
			for (VHost vhost : host2.getVHosts()) {
				if (vhost.getHTDocs() == null) continue;
				vhost.initJLS(new URL[] { vhost.getHTDocs().toURI().toURL() });
				recurLoad(vhost.getJLS(), vhost.getHTDocs()); // TODO: overlapping htdocs may cause some slight delay
			}
			if (sece) {
				recurLoad(null, AvunaHTTPD.fileManager.getPlugin(sec));
			}
		}catch (Exception e) {
			Logger.logError(e);
		}
		config.save();
	}
	
	public void flushjl() {
		try {
			for (JavaLoaderSession jls : sessions) {
				jls.unloadJLCL();
			}
			security.clear();
			secjlcl = null;
			System.gc();
			Thread.sleep(1000L);
			sessions.clear();
			PluginSecurity sec = ((PluginSecurity) registry.getPatchForClass(PluginSecurity.class));
			boolean sece = sec.pcfg.getNode("enabled").getValue().equals("true");
			if (sece) ((PluginSecurity) registry.getPatchForClass(PluginSecurity.class)).loadBases(this);
			HostHTTP host2 = this.registry.host;
			for (VHost vhost : host2.getVHosts()) {
				if (vhost.isChild() || vhost instanceof VHostM) continue;
				vhost.initJLS(new URL[] { vhost.getHTDocs().toURI().toURL() });
				recurLoad(vhost.getJLS(), vhost.getHTDocs()); // TODO: overlapping htdocs may cause some slight delay
			}
			if (sece) {
				recurLoad(null, AvunaHTTPD.fileManager.getPlugin(sec));
			}
			for (JavaLoaderSession session : sessions) {
				if (session.getJLS() != null) for (JavaLoader jl : session.getJLS().values()) {
					jl.postinit();
				}
			}
		}catch (Exception e) {
			Logger.logError(e);
		}
	}
	
	protected static ArrayList<JavaLoaderSession> sessions = new ArrayList<JavaLoaderSession>();
	
	public void recurLoad(JavaLoaderSession session, File dir) {
		for (File f : dir.listFiles()) {
			if (!AvunaHTTPD.windows) {
				try {
					if (SafeMode.isSymlink(f)) {
						continue;
					}
				}catch (CException e) {
					Logger.logError(e);
					continue;
				}
			}
			if (f.isDirectory()) {
				recurLoad(session, f);
			}else {
				try {
					if (f.getName().endsWith(".class")) {
						ByteArrayOutputStream bout = new ByteArrayOutputStream();
						FileInputStream fin = new FileInputStream(f);
						int i = 1;
						byte[] buf = new byte[1024];
						while (i > 0) {
							i = fin.read(buf);
							if (i > 0) {
								bout.write(buf, 0, i);
							}
						}
						fin.close();
						byte[] b = bout.toByteArray();
						bout = null;
						String name = "";
						try {
							name = (session == null ? secjlcl : session.getJLCL()).addClass(b);
						}catch (LinkageError er) {
							// Logger.logError(er);
							continue;
						}
						Class<?> cls = (session == null ? secjlcl : session.getJLCL()).loadClass(name);
						if (JavaLoader.class.isAssignableFrom(cls) && !Modifier.isAbstract(cls.getModifiers())) {
							ConfigNode ocfg = null;
							if (!config.containsNode(session.getVHost().getHostPath())) {
								config.insertNode(session.getVHost().getHostPath());
							}
							ocfg = config.getNode(session.getVHost().getHostPath());
							if (!ocfg.containsNode(name)) {
								ocfg.insertNode(name);
							}
							JavaLoader jl = (JavaLoader) cls.newInstance();
							jl.pcfg = ocfg.getNode(name);
							jl.host = session == null ? null : session.getVHost();
							if (jl.getType() == 3) {
								security.add((JavaLoaderSecurity) jl);
							}else {
								CRC32 crc = new CRC32();
								crc.update(b);
								session.getLoadedClasses().put(crc.getValue() + "", name);
								session.getJLS().put(name, jl);
							}
							jl.init();
						}
					}
				}catch (Exception e) {
					Logger.logError(e);
					Logger.log("Error loading: " + f.getAbsolutePath());
				}
			}
		}
		
	}
	
	public void loadBaseSecurity(JavaLoaderSecurity sec) {
		ConfigNode ocfg = null;
		if (!config.containsNode("security")) {
			config.insertNode("security");
		}
		ocfg = config.getNode("security");
		if (!ocfg.containsNode(sec.getClass().getName())) {
			ocfg.insertNode(sec.getClass().getName());
		}
		sec.pcfg = ocfg.getNode(sec.getClass().getName());
		sec.host = null;
		sec.init();
		security.add(sec);
	}
	
	public static ArrayList<JavaLoaderSecurity> security = new ArrayList<JavaLoaderSecurity>();
	private static JavaLoaderClassLoader secjlcl;
	public static File lib = null;
	
	@Override
	public void formatConfig(ConfigNode json) {
		super.formatConfig(json);
		if (!json.containsNode("lib")) json.insertNode("lib", "lib");
	}
	
	public JavaLoader getFromClass(Class<? extends JavaLoader> cls) {
		for (JavaLoaderSession session : sessions) {
			if (session.getJLS() != null) for (JavaLoader jl : session.getJLS().values()) {
				if (cls.isAssignableFrom(jl.getClass())) {
					return jl;
				}
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void receive(EventBus bus, Event event) {
		if (event instanceof EventGenerateResponse) {
			EventGenerateResponse egr = (EventGenerateResponse) event;
			ResponsePacket response = egr.getResponse();
			RequestPacket request = egr.getRequest();
			if (!(response.headers.hasHeader("Content-Type") && response.headers.getHeader("Content-Type").equals("application/x-java") && response.body != null && response.body != null)) return;
			try {
				response.headers.updateHeader("Content-Type", "text/html");
				String name = "";
				CRC32 crc = new CRC32();
				HashMap<String, String> loadedClasses = request.host.getJLS().getLoadedClasses();
				JavaLoaderClassLoader jlcl = request.host.getJLS().getJLCL();
				HashMap<String, JavaLoader> jls = request.host.getJLS().getJLS();
				crc.update(response.body.data);
				String sha = crc.getValue() + "";
				synchronized (loadedClasses) {
					name = loadedClasses.get(sha);
					if (name == null || name.equals("")) {
						name = jlcl.addClass(response.body.data);
						loadedClasses.put(sha, name);
					}
				}
				JavaLoader loader = null;
				if (!jls.containsKey(name)) {
					Class<?> loaderClass = jlcl.loadClass(name);
					if (loaderClass == null || !JavaLoader.class.isAssignableFrom(loaderClass)) {
						response.body.data = new byte[0];
						return;
					}
					ConfigNode ocfg = null;
					if (!config.containsNode(request.host.getHostPath())) {
						config.insertNode(request.host.getHostPath());
					}
					ocfg = config.getNode(request.host.getHostPath());
					if (!ocfg.containsNode(name)) {
						ocfg.insertNode(name);
					}
					loader = ((Class<? extends JavaLoader>) loaderClass).newInstance();
					loader.pcfg = ocfg.getNode(name);
					loader.host = request.host;
					loader.init();
					jls.put(name, loader);
				}else {
					loader = jls.get(name);
				}
				if (loader == null) {
					response.body.data = new byte[0];
					return;
				}
				request.procJL();
				byte[] ndata = null;
				int type = loader.getType();
				boolean doout = true;
				try {
					request.work.blockTimeout = true;
					if (type == 0) {
						ndata = ((JavaLoaderBasic) loader).generate(response, request);
					}else if (type == 1) {
						HTMLBuilder out = new HTMLBuilder(new StringWriter());
						// long st = System.nanoTime();
						
						doout = ((JavaLoaderPrint) loader).generate(out, response, request);
						// System.out.println((System.nanoTime() - st) / 1000000D);
						String s = out.toString();
						ndata = s.getBytes();
					}else if (type == 2) {
						response.reqStream = (JavaLoaderStream) loader;
					}
				}catch (Exception e) {
					Logger.logError(e);
					ResponseGenerator.generateDefaultResponse(response, StatusCode.INTERNAL_SERVER_ERROR);
					Resource rsc = AvunaHTTPD.fileManager.getErrorPage(request, request.target, StatusCode.INTERNAL_SERVER_ERROR, "Avuna had a critical error attempting to serve your page. Please contact your server administrator and try again. This error has been recorded in the Avuna log file.");
					response.headers.updateHeader("Content-Type", rsc.type);
					response.body = rsc;
					return;
				}finally {
					request.work.blockTimeout = false;
				}
				// System.out.println((digest - start) / 1000000D + " start-digest");
				// System.out.println((loaded - digest) / 1000000D + " digest-loaded");
				// System.out.println((loadert - loaded) / 1000000D + " loaded-loadert");
				// System.out.println((proc - loadert) / 1000000D + " loadert-proc");
				// System.out.println((cur - proc) / 1000000D + " proc-cur");
				response.body.data = !doout ? new byte[0] : ndata;
			}catch (Exception e) {
				Logger.logError(e);
				ResponseGenerator.generateDefaultResponse(response, StatusCode.INTERNAL_SERVER_ERROR);
				Resource rsc = AvunaHTTPD.fileManager.getErrorPage(request, request.target, StatusCode.INTERNAL_SERVER_ERROR, "Avuna had a critical error attempting to serve your page. Please contact your server administrator and try again. This error has been recorded in the Avuna log file.");
				response.headers.updateHeader("Content-Type", rsc.type);
				response.body = rsc;
			}
		}else if (event instanceof EventReload) {
			try {
				// HTMLCache.reloadAll();
				// AssetLibrary.reloadAll();// TODO: if we flushjl at all reloads, then this is usually unneeded?
				config.load();
				flushjl();
				config.save();
			}catch (IOException e) {
				Logger.logError(e);
			}
		}else if (event instanceof EventPreExit) {
			try {
				DatabaseManager.closeAll();
			}catch (SQLException e) {
				Logger.logError(e);
			}
			for (JavaLoader jl : security) {
				jl.destroy();
			}
			for (JavaLoaderSession session : sessions) {
				if (session.getJLS() != null) for (JavaLoader jl : session.getJLS().values()) {
					jl.destroy();
				}
			}
		}else if (event instanceof EventPostInit) {
			for (JavaLoaderSession session : sessions) {
				if (session.getJLS() != null) for (JavaLoader jl : session.getJLS().values()) {
					jl.postinit();
				}
			}
		}
	}
	
	@Override
	public void register(EventBus bus) {
		bus.registerEvent(HTTPEventID.GENERATERESPONSE, this, -500);
		bus.registerEvent(EventID.RELOAD, this, 0);
		bus.registerEvent(EventID.PREEXIT, this, 0);
		bus.registerEvent(EventID.POSTINIT, this, 0);
	}
	
}
