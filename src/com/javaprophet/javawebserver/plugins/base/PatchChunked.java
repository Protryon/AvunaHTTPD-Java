package com.javaprophet.javawebserver.plugins.base;

import java.util.HashMap;
import com.javaprophet.javawebserver.networking.Packet;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.Patch;

public class PatchChunked extends Patch {
	public static PatchChunked INSTANCE;
	
	public PatchChunked(String name) {
		super(name);
		INSTANCE = this;
	}
	
	@Override
	public void formatConfig(HashMap<String, Object> map) {
		if (!map.containsKey("minsize")) map.put("minsize", "10485760");// 10mb
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
		return response.body != null && response.body.getBody() != null && response.body.getBody().tooBig;
	}
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		response.headers.addHeader("Transfer-Encoding", "chunked");
		response.headers.removeHeaders("Content-Length");
		return data;
	}
	
}
