/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins.base;

import java.io.ByteArrayOutputStream;
import java.io.File;
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
import org.avuna.httpd.http.plugins.Plugin;
import org.avuna.httpd.http.plugins.PluginRegistry;

public class PluginGZip extends Plugin {
	
	public PluginGZip(String name, PluginRegistry registry, File config) {
		super(name, registry, config);
	}
	
	private final HashMap<Long, byte[]> pregzip = new HashMap<Long, byte[]>();
	
	@Override
	public void receive(EventBus bus, Event event) {
		if (event instanceof EventGenerateResponse) {
			EventGenerateResponse egr = (EventGenerateResponse) event;
			ResponsePacket response = egr.getResponse();
			RequestPacket request = egr.getRequest();
			String ae = request.headers.getHeader("Accept-Encoding");
			if (ae == null || !ae.contains("gzip")) return;
			if (request.parent != null || response.headers.hasHeader("Content-Encoding") || !response.hasContent() || response.body.tooBig) return;
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
				String cv = response.headers.getHeader("Vary");
				cv = cv == null ? "Accept-Encoding" : (cv + ", " + "Accept-Encoding");
				response.headers.updateHeader("Vary", cv);
			}catch (IOException e) {
				request.host.logger.logError(e);
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
