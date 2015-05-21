package org.avuna.httpd.http.plugins.base;

import org.avuna.httpd.http.ResponseGenerator;
import org.avuna.httpd.http.StatusCode;
import org.avuna.httpd.http.networking.Packet;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.Patch;
import org.avuna.httpd.http.plugins.PatchRegistry;
import org.avuna.httpd.util.ConfigNode;

public class PatchUpgrade extends Patch {
	
	public PatchUpgrade(String name, PatchRegistry registry) {
		super(name, registry);
	}
	
	@Override
	public void formatConfig(ConfigNode json) {
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
		return request.http2Upgrade && request.httpVersion.equals("HTTP/1.1");
	}
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		ResponseGenerator.generateDefaultResponse(response, StatusCode.SWITCHING_PROTOCOLS);
		response.headers.updateHeader("Connection", "Upgrade");
		response.headers.updateHeader("Upgrade", "h2c");
		return new byte[0];
	}
	
	@Override
	public void processMethod(RequestPacket request, ResponsePacket response) {
		
	}
	
}
