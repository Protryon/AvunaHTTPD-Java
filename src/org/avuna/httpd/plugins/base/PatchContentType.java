package org.avuna.httpd.plugins.base;

import java.util.HashMap;
import org.avuna.httpd.http.networking.Packet;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.plugins.Patch;

public class PatchContentType extends Patch {
	
	public PatchContentType(String name) {
		super(name);
	}
	
	@Override
	public void formatConfig(HashMap<String, Object> json) {
		super.formatConfig(json);
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
		return response.body != null;
	}
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		response.headers.removeHeaders("Content-Type");
		if (data != null) {
			String ce = response.body.type;
			response.headers.addHeader("Content-Type", ce.startsWith("text") ? (ce + "; charset=utf-8") : ce);
		}
		return data;
	}
	
	@Override
	public void processMethod(RequestPacket request, ResponsePacket response) {
		
	}
}
