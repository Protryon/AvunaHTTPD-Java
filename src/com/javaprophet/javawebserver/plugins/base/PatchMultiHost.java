package com.javaprophet.javawebserver.plugins.base;

import org.json.simple.JSONObject;
import com.javaprophet.javawebserver.networking.Packet;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.Patch;

public class PatchMultiHost extends Patch {
	
	public PatchMultiHost(String name) {
		super(name);
	}
	
	@Override
	public void formatConfig(JSONObject json) {
		if (!json.containsKey("default")) json.put("default", "");
		if (!json.containsKey("forward")) json.put("forward", new JSONObject());
	}
	
	@Override
	public boolean shouldProcessPacket(Packet packet) {
		return packet instanceof RequestPacket;
	}
	
	@Override
	public void processPacket(Packet packet) {
		RequestPacket request = (RequestPacket)packet;
		String host = request.headers.getHeader("Host");
		JSONObject forward = (JSONObject)pcfg.get("forward");
		for (Object key : forward.keySet()) {
			String[] spl = ((String)key).split(",");
			for (String c : spl) {
				if (c.equals(host)) {
					request.target = forward.get(key) + request.target;
					return;
				}
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
