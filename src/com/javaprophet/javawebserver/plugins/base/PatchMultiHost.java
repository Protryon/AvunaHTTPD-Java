package com.javaprophet.javawebserver.plugins.base;

import java.util.HashMap;
import com.javaprophet.javawebserver.networking.Packet;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.Patch;

public class PatchMultiHost extends Patch {
	
	public PatchMultiHost(String name) {
		super(name);
	}
	
	@Override
	public void formatConfig(HashMap<String, Object> json) {
		if (!json.containsKey("default")) json.put("default", "");
		if (!json.containsKey("forward")) json.put("forward", new HashMap<String, Object>());
	}
	
	@Override
	public boolean shouldProcessPacket(Packet packet) {
		return packet instanceof RequestPacket;
	}
	
	@Override
	public void processPacket(Packet packet) {
		RequestPacket request = (RequestPacket)packet;
		String host = request.headers.getHeader("Host");
		HashMap<String, Object> forward = (HashMap<String, Object>)pcfg.get("forward");
		if (!request.httpVersion.equals("HTTP/1.0")) for (Object key : forward.keySet()) {
			if (host.matches(((String)key))) {
				request.target = forward.get(key) + request.target;
				return;
			}
		}
		request.target = ((String)pcfg.get("default")) + request.target;
	}
	
	@Override
	public boolean shouldProcessResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		return false;
	}
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		return data;
	}
	
	@Override
	public void processMethod(RequestPacket request, ResponsePacket response) {
		
	}
}
