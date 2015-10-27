/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins.avunaagent;

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
import org.avuna.httpd.hosts.VHost;
import org.avuna.httpd.http.Resource;
import org.avuna.httpd.http.ResponseGenerator;
import org.avuna.httpd.http.StatusCode;
import org.avuna.httpd.http.event.EventGenerateResponse;
import org.avuna.httpd.http.event.HTTPEventID;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.Plugin;
import org.avuna.httpd.http.plugins.PluginRegistry;
import org.avuna.httpd.http.plugins.avunaagent.lib.DatabaseManager;
import org.avuna.httpd.http.plugins.security.PluginSecurity;
import org.avuna.httpd.util.CException;
import org.avuna.httpd.util.CLib;
import org.avuna.httpd.util.Config;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.SafeMode;
import org.avuna.httpd.util.unio.UNIOSocket;

public class PluginAvunaAgent extends Plugin {
	
	private boolean disabled = false;
	
	public PluginAvunaAgent(String name, PluginRegistry registry, File config) {
		super(name, registry, config);
		log("Loading AvunaAgent Config & Security");
		PluginSecurity sec = ((PluginSecurity) registry.getPatchForClass(PluginSecurity.class));
		boolean sece = sec != null && sec.pcfg.getNode("enabled").getValue().equals("true");
		if (sece) {
			try {
				secjlcl = new AvunaAgentClassLoader(registry.host, new URL[] { sec.config.toURI().toURL() }, this.getClass().getClassLoader());
			}catch (MalformedURLException e1) {
				e1.printStackTrace();
			}
		}
		if (sece) sec.loadBases(this);
		log("Loading AvunaAgent Libraries");
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
				registry.host.logger.logError(t);
			}
			VHost vhost = (VHost) registry.host;
			if (vhost.isForwarding() || vhost.getHTDocs() == null) {
				disabled = true;
				return;
			}
			vhost.initJLS(this, new URL[] { vhost.getHTDocs().toURI().toURL() });
			recurLoad(vhost.getJLS(), vhost.getHTDocs()); // TODO: overlapping htdocs may cause some slight delay
			if (sece) {
				recurLoad(null, sec.config);
			}
		}catch (Exception e) {
			registry.host.logger.logError(e);
		}
	}
	
	public void clearjl() {
		if (disabled) return;
		for (AvunaAgent jl : security) {
			jl.destroy();
		}
		for (AvunaAgentSession jls : sessions) {
			jls.unloadJLCL();
		}
		security.clear();
		secjlcl = null;
		sessions.clear();
		System.gc();
		try {
			Thread.sleep(100L); // ensure the gc is over
		}catch (InterruptedException e) {
			registry.host.logger.logError(e);
		}
	}
	
	public void flushjl() {
		if (disabled) return;
		try {
			for (AvunaAgent jl : security) {
				jl.destroy();
			}
			for (AvunaAgentSession session : sessions) {
				if (session.getJLS() != null) for (AvunaAgent jl : session.getJLS().values()) {
					jl.destroy();
				}
			}
			for (AvunaAgentSession jls : sessions) {
				jls.unloadJLCL();
			}
			security.clear();
			secjlcl = null;
			System.gc();
			Thread.sleep(100L);
			sessions.clear();
			PluginSecurity sec = ((PluginSecurity) registry.getPatchForClass(PluginSecurity.class));
			boolean sece = sec != null && sec.pcfg.getNode("enabled").getValue().equals("true");
			if (sece) ((PluginSecurity) registry.getPatchForClass(PluginSecurity.class)).loadBases(this);
			VHost vhost = this.registry.host;
			if (vhost.isChild() || vhost.isForwarding()) return;
			vhost.initJLS(this, new URL[] { vhost.getHTDocs().toURI().toURL() });
			recurLoad(vhost.getJLS(), vhost.getHTDocs()); // TODO: overlapping htdocs may cause some slight delay
			if (sece) {
				recurLoad(null, sec.config);
			}
			for (AvunaAgentSession session : sessions) {
				if (session.getJLS() != null) for (AvunaAgent jl : session.getJLS().values()) {
					jl.postinit();
				}
			}
		}catch (Exception e) {
			registry.host.logger.logError(e);
		}
	}
	
	protected ArrayList<AvunaAgentSession> sessions = new ArrayList<AvunaAgentSession>();
	
	public void recurLoad(AvunaAgentSession session, File dir) {
		for (File f : dir.listFiles()) {
			if (!CLib.failed) {
				try {
					if (SafeMode.isSymlink(f)) {
						continue;
					}
				}catch (CException e) {
					session.getVHost().logger.logError(e);
					continue;
				}
			}
			if (f.isDirectory()) {
				recurLoad(session, f);
			}else {
				try {
					if (f.getName().endsWith(".class")) {
						Plugin psec = this.registry.getPatchForClass(PluginSecurity.class);
						if (psec == null && session == null) return;
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
						if (AvunaAgent.class.isAssignableFrom(cls) && !Modifier.isAbstract(cls.getModifiers())) {
							AvunaAgent jl = (AvunaAgent) cls.newInstance();
							String cn = cls.getName();
							cn = cn.substring(cn.lastIndexOf(".") + 1);
							jl.pcfg = new Config(cn, new File(session == null ? (((Config) psec.pcfg).getFile()).getParentFile() : session.getVHost().getHTCfg(), "AvunaAgent/" + cls.getName().replace(".", "/") + ".cfg"), new AvunaAgentConfigFormat(jl) {
								
								@Override
								public void format(ConfigNode map) {
									try {
										us.formatConfig(map);
									}catch (Exception e) {
										registry.host.logger.logError(e);
									}
								}
								
							});
							((Config) jl.pcfg).load();
							((Config) jl.pcfg).save();
							jl.host = session == null ? registry.host : session.getVHost();
							if (jl.getType() == 3) {
								security.add((AvunaAgentSecurity) jl);
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
					session.getVHost().logger.logError(e);
					session.getVHost().logger.log("Error loading: " + f.getAbsolutePath());
				}
			}
		}
		
	}
	
	public void loadBaseSecurity(AvunaAgentSecurity sec) {
		String cn = sec.getClass().getName();
		cn = cn.substring(cn.lastIndexOf(".") + 1);
		Plugin psec = this.registry.getPatchForClass(PluginSecurity.class);
		if (psec == null) return;
		sec.pcfg = new Config(cn, new File(psec.config, "cfg/" + sec.getClass().getName().replace(".", "/") + ".cfg"), new AvunaAgentConfigFormat(sec) {
			
			@Override
			public void format(ConfigNode map) {
				us.formatConfig(map);
			}
			
		});
		try {
			((Config) sec.pcfg).load();
			((Config) sec.pcfg).save();
		}catch (IOException e) {
			registry.host.logger.logError(e);
		}
		sec.host = registry.host;
		sec.init();
		security.add(sec);
	}
	
	public static ArrayList<AvunaAgentSecurity> security = new ArrayList<AvunaAgentSecurity>();
	private static AvunaAgentClassLoader secjlcl;
	public static File lib = null;
	
	@Override
	public void formatConfig(ConfigNode json) {
		super.formatConfig(json);
		if (!json.containsNode("lib")) json.insertNode("lib", "lib");
	}
	
	public AvunaAgent getFromClass(Class<? extends AvunaAgent> cls) {
		for (AvunaAgentSession session : sessions) {
			if (session.getJLS() != null) for (AvunaAgent jl : session.getJLS().values()) {
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
				AvunaAgentClassLoader jlcl = request.host.getJLS().getJLCL();
				HashMap<String, AvunaAgent> jls = request.host.getJLS().getJLS();
				crc.update(response.body.data);
				String sha = crc.getValue() + "";
				synchronized (loadedClasses) {
					name = loadedClasses.get(sha);
					if (name == null || name.equals("")) {
						name = jlcl.addClass(response.body.data);
						loadedClasses.put(sha, name);
					}
				}
				AvunaAgent loader = null;
				if (!jls.containsKey(name)) {
					Class<?> loaderClass = jlcl.loadClass(name);
					if (loaderClass == null || !AvunaAgent.class.isAssignableFrom(loaderClass)) {
						response.body.data = new byte[0];
						return;
					}
					loader = ((Class<? extends AvunaAgent>) loaderClass).newInstance();
					String cn = loaderClass.getName();
					cn = cn.substring(cn.lastIndexOf(".") + 1);
					loader.pcfg = new Config(cn, new File(request.host.getHTCfg(), "AvunaAgent/" + loaderClass.getName().replace(".", "/") + ".cfg"), new AvunaAgentConfigFormat(loader) {
						
						@Override
						public void format(ConfigNode map) {
							us.formatConfig(map);
						}
						
					});
					((Config) loader.pcfg).load();
					((Config) loader.pcfg).save();
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
					if (request.work.s instanceof UNIOSocket) {
						((UNIOSocket) request.work.s).setHoldTimeout(true);
					}
					if (type == 0) {
						ndata = ((AvunaAgentBasic) loader).generate(response, request);
					}else if (type == 1) {
						HTMLBuilder out = new HTMLBuilder(new StringWriter());
						// long st = System.nanoTime();
						
						doout = ((AvunaAgentPrint) loader).generate(out, response, request);
						// System.out.println((System.nanoTime() - st) / 1000000D);
						String s = out.toString();
						ndata = s.getBytes();
					}else if (type == 2) {
						response.reqStream = (AvunaAgentStream) loader;
					}
				}catch (Exception e) {
					request.host.logger.logError(e);
					ResponseGenerator.generateDefaultResponse(response, StatusCode.INTERNAL_SERVER_ERROR);
					Resource rsc = AvunaHTTPD.fileManager.getErrorPage(request, request.target, StatusCode.INTERNAL_SERVER_ERROR, "Avuna had a critical error attempting to serve your page. Please contact your server administrator and try again. This error has been recorded in the Avuna log file.");
					response.headers.updateHeader("Content-Type", rsc.type);
					response.body = rsc;
					return;
				}finally {
					request.work.blockTimeout = false;
					if (request.work.s instanceof UNIOSocket) {
						((UNIOSocket) request.work.s).setHoldTimeout(false);
					}
				}
				// System.out.println((digest - start) / 1000000D + " start-digest");
				// System.out.println((loaded - digest) / 1000000D + " digest-loaded");
				// System.out.println((loadert - loaded) / 1000000D + " loaded-loadert");
				// System.out.println((proc - loadert) / 1000000D + " loadert-proc");
				// System.out.println((cur - proc) / 1000000D + " proc-cur");
				response.body.data = !doout ? new byte[0] : ndata;
			}catch (Exception e) {
				request.host.logger.logError(e);
				ResponseGenerator.generateDefaultResponse(response, StatusCode.INTERNAL_SERVER_ERROR);
				Resource rsc = AvunaHTTPD.fileManager.getErrorPage(request, request.target, StatusCode.INTERNAL_SERVER_ERROR, "Avuna had a critical error attempting to serve your page. Please contact your server administrator and try again. This error has been recorded in the Avuna log file.");
				response.headers.updateHeader("Content-Type", rsc.type);
				response.body = rsc;
			}
		}else if (event instanceof EventPostInit) {
			for (AvunaAgentSession session : sessions) {
				if (session.getJLS() != null) for (AvunaAgent jl : session.getJLS().values()) {
					jl.postinit();
				}
			}
		}
	}
	
	public void destroy() {
		try {
			DatabaseManager.closeAll();
		}catch (SQLException e) {
			registry.host.logger.logError(e);
		}
		clearjl();
	}
	
	@Override
	public void register(EventBus bus) {
		bus.registerEvent(HTTPEventID.GENERATERESPONSE, this, -500);
		bus.registerEvent(EventID.POSTINIT, this, 0);
	}
	
}
