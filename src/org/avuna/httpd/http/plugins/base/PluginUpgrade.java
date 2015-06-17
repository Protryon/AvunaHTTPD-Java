/*	Avuna HTTPD - General Server Applications
    Copyright (C) 2015 Maxwell Bruce

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package org.avuna.httpd.http.plugins.base;

import org.avuna.httpd.event.Event;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.http.ResponseGenerator;
import org.avuna.httpd.http.StatusCode;
import org.avuna.httpd.http.event.EventGenerateResponse;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.Plugin;
import org.avuna.httpd.http.plugins.PluginRegistry;

public class PluginUpgrade extends Plugin {
	
	public PluginUpgrade(String name, PluginRegistry registry) {
		super(name, registry);
	}
	
	@Override
	public void receive(EventBus bus, Event event) {
		if (event instanceof EventGenerateResponse) {
			EventGenerateResponse egr = (EventGenerateResponse)event;
			ResponsePacket response = egr.getResponse();
			RequestPacket request = egr.getRequest();
			if (!(request.http2Upgrade && request.httpVersion.equals("HTTP/1.1"))) return;
			ResponseGenerator.generateDefaultResponse(response, StatusCode.SWITCHING_PROTOCOLS);
			response.headers.updateHeader("Connection", "Upgrade");
			response.headers.updateHeader("Upgrade", "h2c");
			response.body.data = new byte[0];
		}
	}
	
	@Override
	public void register(EventBus bus) {
		// TODO: where do we load this again?
	}
	
}
