package org.avuna.httpd.http.plugins.base;

import java.util.HashMap;
import org.avuna.httpd.http.networking.Packet;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.Patch;

public class PatchChunked extends Patch {
	
	public PatchChunked(String name) {
		super(name);
	}
	
	@Override
	public void formatConfig(HashMap<String, Object> map) {
		super.formatConfig(map);
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
		return request.parent == null && request.httpVersion.equals("HTTP/1.1") && ((response.body != null && response.body.tooBig) || response.reqStream != null);
	}
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		response.headers.addHeader("Transfer-Encoding", "chunked");
		response.headers.removeHeaders("Content-Length");
		return data;
	}
	
}
