package com.javaprophet.javawebserver.plugins.javaloader;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import org.json.simple.JSONObject;
import com.javaprophet.javawebserver.networking.Packet;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.Patch;

public class PatchJavaLoader extends Patch {
	
	public PatchJavaLoader(String name) {
		super(name);
	}
	
	private static MessageDigest sha256 = null;
	static {
		try {
			sha256 = MessageDigest.getInstance("SHA-256");
		}catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void formatConfig(JSONObject json) {
		
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
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		try {
			response.headers.updateHeader("Content-Type", "text/html");
			String name = "";
			String sha = bytesToHex(sha256.digest(data));
			for (String osha : loadedClasses.keySet()) {
				if (osha.equals(sha)) {
					name = loadedClasses.get(osha);
				}
			}
			if (name.equals("")) {
				name = jlcl.addClass(data);
				loadedClasses.put(sha, name);
			}
			Class<? extends JavaLoader> loaderClass = (Class<? extends JavaLoader>)jlcl.loadClass(name);
			if (loaderClass == null) {
				return null;
			}
			JavaLoader loader = loaderClass.newInstance();
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
