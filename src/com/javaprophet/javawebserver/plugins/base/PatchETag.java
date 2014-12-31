package com.javaprophet.javawebserver.plugins.base;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.json.simple.JSONObject;
import com.javaprophet.javawebserver.http.ContentEncoding;
import com.javaprophet.javawebserver.http.Method;
import com.javaprophet.javawebserver.http.StatusCode;
import com.javaprophet.javawebserver.networking.Connection;
import com.javaprophet.javawebserver.networking.Packet;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.Patch;

public class PatchETag extends Patch {
	
	public PatchETag(String name) {
		super(name);
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
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	
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
	public boolean shouldProcessResponse(ResponsePacket response, RequestPacket request, ContentEncoding ce, byte[] data) {
		return response.statusCode == 200 && (request.method == Method.GET || request.method == Method.HEAD) && response.body != null && data != null && data.length > 0;
	}
	
	public static MessageDigest sha256 = null;
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, ContentEncoding ce, byte[] data) {
		if (sha256 == null) return data;
		String etag = bytesToHex(sha256.digest(data));
		if (request.headers.hasHeader("If-None-Match")) {
			if (request.headers.getHeader("If-None-Match").value.replace("\"", "").equals(etag)) {
				Connection.rg.generateDefaultResponse(response, StatusCode.NOT_MODIFIED);
				response.body = null;
				return null;
			}
		}else {
			response.headers.addHeader("ETag", "\"" + etag + "\"");
		}
		return data;
	}
	
}
