package com.javaprophet.javawebserver.plugins.javaloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.json.simple.JSONObject;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.networking.Packet;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.Patch;

public class PatchJavaLoader extends Patch {
	
	public PatchJavaLoader(String name) {
		super(name);
	}
	
	private static MessageDigest md5 = null;
	static {
		try {
			md5 = MessageDigest.getInstance("MD5");
		}catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
	
	private File lib = null;
	
	@Override
	public void formatConfig(JSONObject json) {
		if (!json.containsKey("lib")) json.put("lib", "lib");
		log("Loading JavaLoader Libs");
		lib = new File(JavaWebServer.fileManager.getMainDir(), (String)json.get("lib"));
		if (!lib.exists() || !lib.isDirectory()) {
			lib.mkdirs();
		}
		loadDir(lib);
	}
	
	private void loadDir(File dir) {
		try {
			for (File f : dir.listFiles()) {
				if (f.isDirectory()) {
					loadDir(f);
				}else {
					if (f.getName().endsWith(".class")) {
						FileInputStream fin = new FileInputStream(f);
						ByteArrayOutputStream bout = new ByteArrayOutputStream();
						byte[] buf = new byte[1024];
						int i = 1;
						while (i > 0) {
							i = fin.read(buf);
							if (i > 0) {
								bout.write(buf, 0, i);
							}
						}
						fin.close();
						jlcl.addClass(bout.toByteArray());
					}else if (f.getName().endsWith(".jar")) {
						JarFile jf = new JarFile(f);
						for (JarEntry entry : Collections.list(jf.entries())) {
							if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
								InputStream fin = jf.getInputStream(entry);
								ByteArrayOutputStream bout = new ByteArrayOutputStream();
								byte[] buf = new byte[1024];
								int i = 1;
								while (i > 0) {
									i = fin.read(buf);
									if (i > 0) {
										bout.write(buf, 0, i);
									}
								}
								fin.close();
								jlcl.addClass(bout.toByteArray());
							}
						}
					}
				}
			}
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean shouldProcessPacket(Packet packet) {
		return false;
	}
	
	@Override
	public void processPacket(Packet packet) {
		
	}
	
	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
	
	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
	
	@Override
	public boolean shouldProcessResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		return response.headers.hasHeader("Content-Type") && response.headers.getHeader("Content-Type").value.equals("application/x-java") && response.body != null && data != null && data.length > 0;
	}
	
	private static final HashMap<String, String> loadedClasses = new HashMap<String, String>();
	private static final JavaLoaderClassLoader jlcl = new JavaLoaderClassLoader();
	
	private final HashMap<String, JavaLoader> jls = new HashMap<String, JavaLoader>();
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		try {
			response.headers.updateHeader("Content-Type", "text/html");
			String name = "";
			synchronized (loadedClasses) {
				String sha = bytesToHex(md5.digest(data));
				name = loadedClasses.get(sha);
				if (name == null || name.equals("")) {
					name = jlcl.addClass(data);
					loadedClasses.put(sha, name);
				}
			}
			JavaLoader loader = null;
			if (!jls.containsKey(name)) {
				Class<? extends JavaLoader> loaderClass = (Class<? extends JavaLoader>)jlcl.loadClass(name);
				if (loaderClass == null) {
					return null;
				}
				loader = loaderClass.newInstance();
				jls.put(name, loader);
			}else {
				loader = jls.get(name);
			}
			if (loader == null) return null;
			request.procJL();
			byte[] ndata = loader.generate(response, request);
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
