package com.javaprophet.javawebserver.plugins.base;

import org.json.simple.JSONObject;
import com.javaprophet.javawebserver.networking.Packet;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.Patch;

public class PatchContentLength extends Patch {
	
	public PatchContentLength(String name) {
		super(name);
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
	
	@Override
	public boolean shouldProcessResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		return true;
	}
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		response.headers.removeHeaders("Content-Length");
		if (response.headers.hasHeader("Transfer-Encoding")) {
			// do we even need this?
			response.headers.removeHeaders("Transfer-Encoding");
		}
		if (data != null) response.headers.addHeader("Content-Length", data.length + "");
		return data;
	}
	
	@Override
	public void processMethod(RequestPacket request, ResponsePacket response) {
		
	}
}
