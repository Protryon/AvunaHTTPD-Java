/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins.base;

import java.util.zip.CRC32;
import org.avuna.httpd.event.Event;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.http.Method;
import org.avuna.httpd.http.ResponseGenerator;
import org.avuna.httpd.http.StatusCode;
import org.avuna.httpd.http.event.EventGenerateResponse;
import org.avuna.httpd.http.event.HTTPEventID;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.Plugin;
import org.avuna.httpd.http.plugins.PluginRegistry;

public class PluginETag extends Plugin {
	
	public PluginETag(String name, PluginRegistry registry) {
		super(name, registry);
	}
	
	@Override
	public void receive(EventBus bus, Event event) {
		if (event instanceof EventGenerateResponse) {
			EventGenerateResponse egr = (EventGenerateResponse) event;
			ResponsePacket response = egr.getResponse();
			RequestPacket request = egr.getRequest();
			if ((request.parent != null || response.statusCode != 200 || (request.method != Method.GET && request.method != Method.HEAD) || response.body == null || response.body.data == null || response.body.data.length == 0)) return;
			CRC32 crc = new CRC32();
			crc.update(request.body.data);
			String etag = crc.getValue() + "";// bytesToHex(md5.digest(data));
			if (request.headers.hasHeader("If-None-Match")) {
				if (request.headers.getHeader("If-None-Match").replace("\"", "").equals(etag)) {
					ResponseGenerator.generateDefaultResponse(response, StatusCode.NOT_MODIFIED);
					response.body = null;
				}
			}else {
				response.headers.addHeader("ETag", "\"" + etag + "\"");
			}
		}
	}
	
	@Override
	public void register(EventBus bus) {
		bus.registerEvent(HTTPEventID.GENERATERESPONSE, this, -700);
	}
	
}
