package org.avuna.httpd.plugins.base;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.zip.CRC32;
import java.util.zip.GZIPOutputStream;
import org.avuna.httpd.http.networking.Packet;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.plugins.Patch;
import org.avuna.httpd.util.Logger;

public class PatchGZip extends Patch {
	
	public PatchGZip(String name) {
		super(name);
	}
	
	@Override
	public void formatConfig(HashMap<String, Object> json) {
		
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
		return request.parent == null && request.headers.hasHeader("Accept-Encoding") && request.headers.getHeader("Accept-Encoding").contains("gzip") && !response.headers.hasHeader("Content-Encoding") && ((data != null && data.length > 0) || (response.body != null && response.body.tooBig));
	}
	
	private final HashMap<Long, byte[]> pregzip = new HashMap<Long, byte[]>();
	
	public void clearCache() {
		pregzip.clear();
	}
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		byte[] data2 = data;
		try {
			if (data != null && data.length > 0) {
				CRC32 crc = new CRC32();
				crc.update(data);
				long l = crc.getValue();
				if (pregzip.containsKey(l)) {
					data2 = pregzip.get(l);
				}else {
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					GZIPOutputStream gout = new GZIPOutputStream(bout);
					gout.write(data, 0, data.length);
					gout.flush();
					gout.close();
					data2 = bout.toByteArray();
					pregzip.put(l, data2);
				}
			}
			response.headers.addHeader("Content-Encoding", "gzip");
			response.headers.addHeader("Vary", "Accept-Encoding");
		}catch (IOException e) {
			Logger.logError(e);
		}
		return data2;
	}
	
	@Override
	public void processMethod(RequestPacket request, ResponsePacket response) {
		
	}
	
}
