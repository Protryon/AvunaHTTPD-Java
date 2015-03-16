package com.javaprophet.javawebserver.plugins.base;

import java.util.HashMap;
import com.javaprophet.javawebserver.networking.packets.Packet;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.Patch;

public class PatchCacheControl extends Patch {
	
	public PatchCacheControl(String name) {
		super(name);
		reload();
	}
	
	private String[] cache = null;
	private int maxAge = 604800;
	
	@Override
	public void formatConfig(HashMap<String, Object> json) {
		if (!json.containsKey("maxage")) json.put("maxage", "604800");
		if (!json.containsKey("cache")) json.put("cache", "text/css;application/javascript;image/*");
	}
	
	public void reload() {
		maxAge = Integer.parseInt((String)pcfg.get("maxage"));
		cache = ((String)pcfg.get("cache")).split(";");
	}
	
	@Override
	public boolean shouldProcessPacket(Packet packet) {
		return false;
	}
	
	@Override
	public void processPacket(Packet packet) {
		
	}
	
	@Override
	public void processMethod(RequestPacket request, ResponsePacket response) {
		
	}
	
	@Override
	public boolean shouldProcessResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		return request.parent == null && response.body != null && response.headers.hasHeader("Content-Type");
	}
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		String ct = response.headers.getHeader("Content-Type");
		if (ct.contains(";")) ct = ct.substring(0, ct.indexOf(";")).trim();
		boolean nc = true;
		for (String s : cache) {
			if (!s.endsWith("*") && s.equals(ct)) {
				nc = false;
				break;
			}else if (s.endsWith("*") && ct.startsWith(s.substring(0, s.length() - 1))) {
				nc = false;
				break;
			}
		}
		response.headers.addHeader("Cache-Control: max-age=" + maxAge + (nc ? ", no-cache" : ""));
		return data;
	}
}
