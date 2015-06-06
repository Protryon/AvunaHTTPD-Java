package org.avuna.httpd.http.plugins.base;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.zip.CRC32;
import java.util.zip.GZIPOutputStream;
import org.avuna.httpd.event.Event;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.http.event.EventClearCache;
import org.avuna.httpd.http.event.EventGenerateResponse;
import org.avuna.httpd.http.event.HTTPEventID;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.Patch;
import org.avuna.httpd.http.plugins.PatchRegistry;
import org.avuna.httpd.util.Logger;

public class PatchGZip extends Patch {
	
	public PatchGZip(String name, PatchRegistry registry) {
		super(name, registry);
	}
	
	private final HashMap<Long, byte[]> pregzip = new HashMap<Long, byte[]>();
	
	@Override
	public void receive(EventBus bus, Event event) {
		if (event instanceof EventGenerateResponse) {
			EventGenerateResponse egr = (EventGenerateResponse)event;
			ResponsePacket response = egr.getResponse();
			RequestPacket request = egr.getRequest();
			if (!(request.parent == null && request.headers.hasHeader("Accept-Encoding") && request.headers.getHeader("Accept-Encoding").contains("gzip") && !response.headers.hasHeader("Content-Encoding") && response.body != null && !response.body.tooBig)) return;
			byte[] data2 = response.body.data;
			try {
				CRC32 crc = new CRC32();
				crc.update(data2);
				long l = crc.getValue();
				if (pregzip.containsKey(l)) {
					data2 = pregzip.get(l);
				}else {
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					GZIPOutputStream gout = new GZIPOutputStream(bout);
					gout.write(data2, 0, data2.length);
					gout.flush();
					gout.close();
					data2 = bout.toByteArray();
					pregzip.put(l, data2);
				}
				response.headers.addHeader("Content-Encoding", "gzip");
				response.headers.addHeader("Vary", "Accept-Encoding");
			}catch (IOException e) {
				Logger.logError(e);
			}
			response.body.data = data2;
		}else if (event instanceof EventClearCache) {
			pregzip.clear();
		}
	}
	
	@Override
	public void register(EventBus bus) {
		bus.registerEvent(HTTPEventID.GENERATERESPONSE, this, -800);
		bus.registerEvent(HTTPEventID.CLEARCACHE, this, 0);
	}
	
}
