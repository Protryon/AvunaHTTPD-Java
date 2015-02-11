package com.javaprophet.javawebserver.plugins.base;

import java.util.HashMap;
import com.javaprophet.javawebserver.networking.packets.Packet;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.Patch;

public class PatchEnforceRedirect extends Patch {
	
	public PatchEnforceRedirect(String name) {
		super(name);
	}
	
	@Override
	public void formatConfig(HashMap<String, Object> json) {
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
		return true;
	}
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		String host = request.headers.getHeader("Host");
		if (!request.httpVersion.equals("HTTP/1.0")) for (String key : pcfg.keySet(request)) {
			if (key.equals("enabled")) continue;
			String regex = key;
			if (!host.equals(pcfg.get(key, request)) && host.matches(regex)) {
				response.headers.updateHeader("Location", (request.ssl ? "https" : "http") + "://" + (String)pcfg.get(key, request) + request.target);
				response.statusCode = 301;
				response.reasonPhrase = "Moved Permanently";
				response.body = null;
				return null;
			}
		}
		return data;
	}
	
	@Override
	public void processMethod(RequestPacket request, ResponsePacket response) {
		
	}
}
