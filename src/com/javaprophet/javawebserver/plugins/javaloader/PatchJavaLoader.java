package com.javaprophet.javawebserver.plugins.javaloader;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.networking.Packet;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.Patch;

public class PatchJavaLoader extends Patch {
	
	public PatchJavaLoader(String name) {
		super(name);
		log("Loading JavaLoader Libs");
		lib = new File(JavaWebServer.fileManager.getMainDir(), (String)pcfg.get("lib"));
		if (!lib.exists() || !lib.isDirectory()) {
			lib.mkdirs();
		}
		URLClassLoader sysloader = (URLClassLoader)ClassLoader.getSystemClassLoader();
		Class<?> sysclass = URLClassLoader.class;
		try {
			Method method = sysclass.getDeclaredMethod("addURL", URL.class);
			method.setAccessible(true);
			method.invoke(sysloader, new Object[]{lib.toURI().toURL()});
			for (File f : lib.listFiles()) {
				if (!f.isDirectory() && f.getName().endsWith(".jar")) {
					method.invoke(sysloader, new Object[]{f.toURI().toURL()});
				}
				method.invoke(sysloader, new Object[]{JavaWebServer.fileManager.getHTDocs().toURI().toURL()});
			}
		}catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	private static MessageDigest md5 = null;
	static {
		try {
			md5 = MessageDigest.getInstance("MD5");
		}catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
	
	public static File lib = null;
	
	@Override
	public void formatConfig(HashMap<String, Object> json) {
		if (!json.containsKey("lib")) json.put("lib", "lib");
	}
	
	public void preExit() {
		super.preExit();
		for (JavaLoader jl : jls.values()) {
			jl.destroy();
		}
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
	
	private static final HashMap<String, String> loadedClasses = new HashMap<String, String>();
	private static final JavaLoaderClassLoader jlcl = new JavaLoaderClassLoader();
	
	private final HashMap<String, JavaLoader> jls = new HashMap<String, JavaLoader>();
	
	public JavaLoader getFromClass(Class<? extends JavaLoader> cls) {
		for (JavaLoader jl : jls.values()) {
			if (cls.isAssignableFrom(jl.getClass())) {
				return jl;
			}
		}
		return null;
	}
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		try {
			response.headers.updateHeader("Content-Type", "text/html");
			String name = "";
			long start = System.nanoTime();
			long digest = 0L;
			synchronized (loadedClasses) {
				String sha = data.hashCode() + "";
				digest = System.nanoTime();
				name = loadedClasses.get(sha);
				if (name == null || name.equals("")) {
					name = jlcl.addClass(data);
					loadedClasses.put(sha, name);
				}
			}
			long loaded = System.nanoTime();
			JavaLoader loader = null;
			if (!jls.containsKey(name)) {
				Class<?> loaderClass = jlcl.loadClass(name);
				if (loaderClass == null || !JavaLoader.class.isAssignableFrom(loaderClass)) {
					return null;
				}
				loader = ((Class<? extends JavaLoader>)loaderClass).newInstance();
				jls.put(name, loader);
			}else {
				loader = jls.get(name);
			}
			if (loader == null) return null;
			long loadert = System.nanoTime();
			request.procJL();
			long proc = System.nanoTime();
			byte[] ndata = loader.generate(response, request);
			long cur = System.nanoTime();
			// System.out.println((digest - start) / 1000000D + " start-digest");
			// System.out.println((loaded - digest) / 1000000D + " digest-loaded");
			// System.out.println((loadert - loaded) / 1000000D + " loaded-loadert");
			// System.out.println((proc - loadert) / 1000000D + " loadert-proc");
			// System.out.println((cur - proc) / 1000000D + " proc-cur");
			return ndata;
		}catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public void processMethod(RequestPacket request, ResponsePacket response) {
		
	}
}
