package org.avuna.httpd.http.plugins.javaloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.CRC32;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.Host;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.hosts.VHost;
import org.avuna.httpd.hosts.VHostM;
import org.avuna.httpd.http.Resource;
import org.avuna.httpd.http.ResponseGenerator;
import org.avuna.httpd.http.StatusCode;
import org.avuna.httpd.http.networking.Packet;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.Patch;
import org.avuna.httpd.http.plugins.PatchRegistry;
import org.avuna.httpd.http.plugins.base.PatchSecurity;
import org.avuna.httpd.http.plugins.javaloader.lib.DatabaseManager;
import org.avuna.httpd.http.plugins.javaloader.lib.HTMLCache;
import org.avuna.httpd.util.Config;
import org.avuna.httpd.util.ConfigFormat;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.Logger;

public class PatchJavaLoader extends Patch {
	
	private Config config = null;
	
	public void postload() {
		for (JavaLoaderSession session : sessions) {
			if (session.getJLS() != null) for (JavaLoader jl : session.getJLS().values()) {
				jl.postinit();
			}
		}
	}
	
	public PatchJavaLoader(String name, PatchRegistry registry) {
		super(name, registry);
		log("Loading JavaLoader Config & Security");
		try {
			secjlcl = new JavaLoaderClassLoader(new URL[]{AvunaHTTPD.fileManager.getPlugin(registry.getPatchForClass(PatchSecurity.class)).toURI().toURL()}, this.getClass().getClassLoader());
		}catch (MalformedURLException e1) {
			e1.printStackTrace();
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
		((PatchSecurity)registry.getPatchForClass(PatchSecurity.class)).loadBases(this);
		log("Loading JavaLoader Libs");
		try {
			lib = new File(AvunaHTTPD.fileManager.getMainDir(), pcfg.getNode("lib").getValue());
			if (!lib.exists() || !lib.isDirectory()) {
				lib.mkdirs();
			}
			URLClassLoader sysloader = (URLClassLoader)ClassLoader.getSystemClassLoader();
			Class<URLClassLoader> sysclass = URLClassLoader.class;
			
			try {
				Method method = sysclass.getDeclaredMethod("addURL", URL.class);
				method.setAccessible(true);
				method.invoke(sysloader, new Object[]{lib.toURI().toURL()});
				for (File f : lib.listFiles()) {
					if (!f.isDirectory() && f.getName().endsWith(".jar")) {
						method.invoke(sysloader, new Object[]{f.toURI().toURL()});
					}
				}
			}catch (Throwable t) {
				Logger.logError(t);
			}
			HostHTTP host2 = (HostHTTP)registry.host;
			for (VHost vhost : host2.getVHosts()) {
				if (vhost.getHTDocs() == null) continue;
				vhost.initJLS(new URL[]{vhost.getHTDocs().toURI().toURL()});
				recurLoad(vhost.getJLS(), vhost.getHTDocs()); // TODO: overlapping htdocs may cause some slight delay
			}
			PatchSecurity ps = (PatchSecurity)registry.getPatchForClass(PatchSecurity.class);
			if (ps.pcfg.getNode("enabled").getValue().equals("true")) {
				recurLoad(null, AvunaHTTPD.fileManager.getPlugin(ps));
			}
		}catch (Exception e) {
			Logger.logError(e);
		}
		config.save();
	}
	
	public void saveConfig() {
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
			((PatchSecurity)registry.getPatchForClass(PatchSecurity.class)).loadBases(this);
			for (Host host : AvunaHTTPD.hosts.values()) {
				if (!(host instanceof HostHTTP)) continue;
				HostHTTP host2 = (HostHTTP)host;
				for (VHost vhost : host2.getVHosts()) {
					if (vhost.isChild() || vhost instanceof VHostM) continue;
					vhost.initJLS(new URL[]{vhost.getHTDocs().toURI().toURL()});
					recurLoad(vhost.getJLS(), vhost.getHTDocs()); // TODO: overlapping htdocs may cause some slight delay
				}
			}
			PatchSecurity ps = (PatchSecurity)registry.getPatchForClass(PatchSecurity.class);
			if (ps.pcfg.getNode("enabled").getValue().equals("true")) {
				recurLoad(null, AvunaHTTPD.fileManager.getPlugin(ps));
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
						if (JavaLoader.class.isAssignableFrom(cls)) {
							ConfigNode ocfg = null;
							if (!config.containsNode(session.getVHost().getHostPath())) {
								config.insertNode(session.getVHost().getHostPath());
							}
							ocfg = config.getNode(session.getVHost().getHostPath());
							if (!ocfg.containsNode(name)) {
								ocfg.insertNode(name);
							}
							JavaLoader jl = (JavaLoader)cls.newInstance();
							jl.pcfg = ocfg.getNode(name);
							jl.host = session == null ? null : session.getVHost();
							if (jl.getType() == 3) {
								security.add((JavaLoaderSecurity)jl);
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
	
	public void preExit() {
		super.preExit();
		try {
			DatabaseManager.closeAll();
		}catch (SQLException e) {
			Logger.logError(e);
		}
		config.save();
		for (JavaLoader jl : security) {
			jl.destroy();
		}
		for (JavaLoaderSession session : sessions) {
			if (session.getJLS() != null) for (JavaLoader jl : session.getJLS().values()) {
				jl.destroy();
			}
		}
	}
	
	public void reload() throws IOException {
		super.reload();
		HTMLCache.reloadAll();
		config.load();
		flushjl();
		// for (JavaLoaderSession session : sessions) {
		// if (session.getJLS() != null) for (JavaLoader jl : session.getJLS().values()) {
		// ConfigNode ocfg = null;
		// if (!config.containsNode(session.getVHost().getHostPath())) {
		// config.insertNode(session.getVHost().getHostPath());
		// }
		// ocfg = config.getNode(session.getVHost().getHostPath());
		// if (!ocfg.containsNode(jl.getClass().getName())) {
		// ocfg.insertNode(jl.getClass().getName());
		// }
		// jl.pcfg = ocfg.getNode(jl.getClass().getName());
		// jl.host = session == null ? null : session.getVHost();
		// jl.reload();
		// }
		// }
	}
	
	@Override
	public boolean shouldProcessPacket(Packet packet) {
		return false;
	}
	
	@Override
	public void processPacket(Packet packet) {
		
	}
	
	@Override
	public boolean shouldProcessResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		return response.headers.hasHeader("Content-Type") && response.headers.getHeader("Content-Type").equals("application/x-java") && response.body != null && data != null && data.length > 0;
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
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		try {
			response.headers.updateHeader("Content-Type", "text/html");
			String name = "";
			CRC32 crc = new CRC32();
			HashMap<String, String> loadedClasses = request.host.getJLS().getLoadedClasses();
			JavaLoaderClassLoader jlcl = request.host.getJLS().getJLCL();
			HashMap<String, JavaLoader> jls = request.host.getJLS().getJLS();
			crc.update(data);
			String sha = crc.getValue() + "";
			synchronized (loadedClasses) {
				name = loadedClasses.get(sha);
				if (name == null || name.equals("")) {
					name = jlcl.addClass(data);
					loadedClasses.put(sha, name);
				}
			}
			JavaLoader loader = null;
			if (!jls.containsKey(name)) {
				Class<?> loaderClass = jlcl.loadClass(name);
				if (loaderClass == null || !JavaLoader.class.isAssignableFrom(loaderClass)) {
					return null;
				}
				ConfigNode ocfg = null;
				if (!config.containsNode(request.host.getHostPath())) {
					config.insertNode(request.host.getHostPath());
				}
				ocfg = config.getNode(request.host.getHostPath());
				if (!ocfg.containsNode(name)) {
					ocfg.insertNode(name);
				}
				loader = ((Class<? extends JavaLoader>)loaderClass).newInstance();
				loader.pcfg = ocfg.getNode(name);
				loader.host = request.host;
				loader.init();
				jls.put(name, loader);
			}else {
				loader = jls.get(name);
			}
			if (loader == null) return null;
			request.procJL();
			byte[] ndata = null;
			int type = loader.getType();
			boolean doout = true;
			try {
				request.work.blockTimeout = true;
				if (type == 0) {
					ndata = ((JavaLoaderBasic)loader).generate(response, request);
				}else if (type == 1) {
					HTMLBuilder out = new HTMLBuilder(new StringWriter());
					// long st = System.nanoTime();
					
					doout = ((JavaLoaderPrint)loader).generate(out, response, request);
					// System.out.println((System.nanoTime() - st) / 1000000D);
					String s = out.toString();
					ndata = s.getBytes();
				}else if (type == 2) {
					response.reqStream = (JavaLoaderStream)loader;
				}
			}catch (Exception e) {
				Logger.logError(e);
				ResponseGenerator.generateDefaultResponse(response, StatusCode.INTERNAL_SERVER_ERROR);
				Resource rsc = AvunaHTTPD.fileManager.getErrorPage(request, request.target, StatusCode.INTERNAL_SERVER_ERROR, "Avuna had a critical error attempting to serve your page. Please contact your server administrator and try again. This error has been recorded in the Avuna log file.");
				response.headers.updateHeader("Content-Type", rsc.type);
				return rsc.data;
			}finally {
				request.work.blockTimeout = false;
			}
			// System.out.println((digest - start) / 1000000D + " start-digest");
			// System.out.println((loaded - digest) / 1000000D + " digest-loaded");
			// System.out.println((loadert - loaded) / 1000000D + " loaded-loadert");
			// System.out.println((proc - loadert) / 1000000D + " loadert-proc");
			// System.out.println((cur - proc) / 1000000D + " proc-cur");
			return !doout ? new byte[0] : ndata;
		}catch (Exception e) {
			Logger.logError(e);
		}
		return null;
	}
	
	@Override
	public void processMethod(RequestPacket request, ResponsePacket response) {
		
	}
}
