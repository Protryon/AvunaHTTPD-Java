package org.avuna.httpd.http.plugins.base;

import org.avuna.httpd.event.Event;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.http.ResponseGenerator;
import org.avuna.httpd.http.StatusCode;
import org.avuna.httpd.http.event.EventGenerateResponse;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.Patch;
import org.avuna.httpd.http.plugins.PatchRegistry;

public class PatchUpgrade extends Patch {
	
	public PatchUpgrade(String name, PatchRegistry registry) {
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
