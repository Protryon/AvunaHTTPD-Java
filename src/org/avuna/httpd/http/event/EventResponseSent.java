package org.avuna.httpd.http.event;

import org.avuna.httpd.event.Event;
import org.avuna.httpd.http.networking.ResponsePacket;

public class EventResponseSent extends Event {
	private final ResponsePacket response;
	
	public ResponsePacket getResponse() {
		return response;
	}
	
	public EventResponseSent(ResponsePacket response) {
		super(HTTPEventID.RESPONSESENT);
		this.response = response;
	}
	
}
