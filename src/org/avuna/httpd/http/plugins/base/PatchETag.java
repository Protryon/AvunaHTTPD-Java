package org.avuna.httpd.http.plugins.base;

import java.util.zip.CRC32;
import org.avuna.httpd.http.Method;
import org.avuna.httpd.http.ResponseGenerator;
import org.avuna.httpd.http.StatusCode;
import org.avuna.httpd.http.networking.Packet;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.Patch;
import org.avuna.httpd.http.plugins.PatchRegistry;
import org.avuna.httpd.util.ConfigNode;

public class PatchETag extends Patch {
	
	public PatchETag(String name, PatchRegistry registry) {
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
		return request.parent == null && response.statusCode == 200 && (request.method == Method.GET || request.method == Method.HEAD) && response.body != null && data != null && data.length > 0;
	}
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		CRC32 crc = new CRC32();
		crc.update(data);
		String etag = crc.getValue() + "";// bytesToHex(md5.digest(data));
		if (request.headers.hasHeader("If-None-Match")) {
			if (request.headers.getHeader("If-None-Match").replace("\"", "").equals(etag)) {
				ResponseGenerator.generateDefaultResponse(response, StatusCode.NOT_MODIFIED);
				response.body = null;
				return null;
			}
		}else {
			response.headers.addHeader("ETag", "\"" + etag + "\"");
		}
		return data;
	}
	
	@Override
	public void processMethod(RequestPacket request, ResponsePacket response) {
		
	}
	
}
