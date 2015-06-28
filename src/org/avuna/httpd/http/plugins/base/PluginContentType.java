/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins.base;

import org.avuna.httpd.event.Event;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.http.event.EventGenerateResponse;
import org.avuna.httpd.http.event.HTTPEventID;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.Plugin;
import org.avuna.httpd.http.plugins.PluginRegistry;

public class PluginContentType extends Plugin {
	
	public PluginContentType(String name, PluginRegistry registry) {
		super(name, registry);
	}
	
	@Override
	public void receive(EventBus bus, Event event) {
		if (event instanceof EventGenerateResponse) {
			EventGenerateResponse egr = (EventGenerateResponse) event;
			ResponsePacket response = egr.getResponse();
			if (response.body == null) {
				response.headers.removeHeaders("Content-Type");
				return;
			}
			String ce = response.body.type;
			if (ce == null) ce = "text/html; charset=utf-8";
			if (ce.startsWith("text")) {
				if (!ce.contains(";")) {// TODO: make safe
					ce += "; charset=utf-8";
				}
			}
			response.headers.updateHeader("Content-Type", ce.startsWith("text") ? (ce + "; charset=utf-8") : ce);
		}
	}
	
	@Override
	public void register(EventBus bus) {
		bus.registerEvent(HTTPEventID.GENERATERESPONSE, this, 999);
	}
	
}
