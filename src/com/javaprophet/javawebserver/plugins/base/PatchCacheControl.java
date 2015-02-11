package com.javaprophet.javawebserver.plugins.base;

import java.util.HashMap;
import com.javaprophet.javawebserver.networking.packets.Packet;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.Patch;

public class PatchCacheControl extends Patch {
	
	public PatchCacheControl(String name) {
		super(name);
	}
	
	@Override
	public void formatConfig(HashMap<String, Object> json) {
		if (!json.containsKey("maxage")) json.put("maxage", "604800");
		if (!json.containsKey("nocache")) json.put("nocache", "application/.*");
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
		return response.body != null;
	}
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		response.headers.addHeader("Cache-Control: max-age=" + (String)pcfg.get("maxage", request) + (response.headers.getHeader("Content-Type").matches((String)pcfg.get("nocache", request)) ? ", no-cache" : ""));
		return data;
	}
	
}
