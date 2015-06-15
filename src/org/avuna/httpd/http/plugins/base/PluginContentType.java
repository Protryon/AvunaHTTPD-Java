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
			EventGenerateResponse egr = (EventGenerateResponse)event;
			ResponsePacket response = egr.getResponse();
			if (response.body == null) {
				response.headers.removeHeaders("Content-Type");
				return;
			}
			String ce = response.body.type;
			response.headers.updateHeader("Content-Type", ce.startsWith("text") ? (ce + "; charset=utf-8") : ce);
		}
	}
	
	@Override
	public void register(EventBus bus) {
		bus.registerEvent(HTTPEventID.GENERATERESPONSE, this, 999);
	}
	
}
