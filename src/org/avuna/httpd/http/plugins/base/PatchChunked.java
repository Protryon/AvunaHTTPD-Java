package org.avuna.httpd.http.plugins.base;

import org.avuna.httpd.http.networking.Packet;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.Patch;
import org.avuna.httpd.http.plugins.PatchRegistry;
import org.avuna.httpd.util.ConfigNode;

public class PatchChunked extends Patch {
	
	public PatchChunked(String name, PatchRegistry registry) {
		super(name, registry);
	}
	
	@Override
	public void formatConfig(ConfigNode map) {
		super.formatConfig(map);
		if (!map.containsNode("minsize")) map.insertNode("minsize", "10485760");// 10mb
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
		return request.parent == null && request.httpVersion.equals("HTTP/1.1") && ((response.body != null && response.body.tooBig) || response.reqStream != null);
	}
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		response.headers.addHeader("Transfer-Encoding", "chunked");
		response.headers.removeHeaders("Content-Length");
		return data;
	}
	
}
