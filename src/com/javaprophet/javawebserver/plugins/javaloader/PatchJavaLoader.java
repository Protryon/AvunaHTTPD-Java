package com.javaprophet.javawebserver.plugins.javaloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.zip.CRC32;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.networking.Packet;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.Patch;
import com.javaprophet.javawebserver.util.Logger;

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
			}
			method.invoke(sysloader, new Object[]{JavaWebServer.fileManager.getHTDocs().toURI().toURL()});
		}catch (Throwable t) {
			Logger.logError(t);;
		}
		recurLoad(JavaWebServer.fileManager.getHTDocs());
	}
	
	public void recurLoad(File dir) {
		try {
			for (File f : dir.listFiles()) {
				if (f.isDirectory()) {
					recurLoad(f);
				}else {
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
						byte[] b = bout.toByteArray();
						bout = null;
						String name = "";
						try {
							name = jlcl.addClass(b);
						}catch (LinkageError er) {
							String msg = er.getMessage();
							if (msg.contains("duplicate class definition for name")) {
								String type = msg.substring(msg.indexOf("\"") + 1);
								type = type.substring(0, type.indexOf("\"")).replace("/", ".");
								name = type;
							}else {
								continue;
							}
						}
						Class<?> cls = jlcl.loadClass(name);
						if (JavaLoader.class.isAssignableFrom(cls)) {
							CRC32 crc = new CRC32();
							crc.update(b);
							loadedClasses.put(crc.getValue() + "", name);
							JavaLoader jl = (JavaLoader)cls.newInstance();
							jls.put(name, jl);
						}
					}
				}
			}
		}catch (Exception e) {
			Logger.logError(e);;
		}
	}
	
	private static MessageDigest md5 = null;
	static {
		try {
			md5 = MessageDigest.getInstance("MD5");
		}catch (NoSuchAlgorithmException e) {
			Logger.logError(e);;
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
	
	public void reload() {
		super.reload();
		for (JavaLoader jl : jls.values()) {
			jl.reload();
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
	private static JavaLoaderClassLoader jlcl = new JavaLoaderClassLoader();
	
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
			CRC32 crc = new CRC32();
			synchronized (loadedClasses) {
				crc.update(data);
				String sha = crc.getValue() + "";
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
			byte[] ndata = null;
			int type = loader.getType();
			if (type == 0) {
				ndata = ((JavaLoaderBasic)loader).generate(response, request);
			}else if (type == 1) {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				PrintStream out = new PrintStream(bout);
				// long st = System.nanoTime();
				((JavaLoaderPrint)loader).generate(out, response, request);
				// System.out.println((System.nanoTime() - st) / 1000000D);
				ndata = bout.toByteArray();
			}else if (type == 2) {
				response.reqStream = (JavaLoaderStream)loader;
			}
			long cur = System.nanoTime();
			// System.out.println((digest - start) / 1000000D + " start-digest");
			// System.out.println((loaded - digest) / 1000000D + " digest-loaded");
			// System.out.println((loadert - loaded) / 1000000D + " loaded-loadert");
			// System.out.println((proc - loadert) / 1000000D + " loadert-proc");
			// System.out.println((cur - proc) / 1000000D + " proc-cur");
			return ndata;
		}catch (Exception e) {
			Logger.logError(e);;
		}
		return null;
	}
	
	@Override
	public void processMethod(RequestPacket request, ResponsePacket response) {
		
	}
}
