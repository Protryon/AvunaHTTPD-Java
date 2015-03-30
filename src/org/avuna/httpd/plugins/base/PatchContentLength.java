package org.avuna.httpd.plugins.base;

import java.util.HashMap;
import org.avuna.httpd.http.networking.Packet;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.plugins.Patch;

public class PatchContentLength extends Patch {
	
	public PatchContentLength(String name) {
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
		return !response.headers.hasHeader("Transfer-Encoding") && response.body != null && data != null;
	}
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		response.headers.removeHeaders("Content-Length");
		if (data != null) {
			response.headers.addHeader("Content-Length", data.length + "");
			if (!response.headers.hasHeader("Content-Type")) response.headers.addHeader("Content-Type", response.body.type);
		}
		return data;
	}
	
	@Override
	public void processMethod(RequestPacket request, ResponsePacket response) {
		
	}
}
