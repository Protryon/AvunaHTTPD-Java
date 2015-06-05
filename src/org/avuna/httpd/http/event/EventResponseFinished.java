package org.avuna.httpd.http.event;

import org.avuna.httpd.event.Event;
import org.avuna.httpd.http.networking.ResponsePacket;

public class EventResponseFinished extends Event {
	
	private final ResponsePacket response;
	
	public ResponsePacket getResponse() {
		return response;
	}
	
	public EventResponseFinished(ResponsePacket response) {
		super(HTTPEventID.RESPONSEFINISHED);
		this.response = response;
	}
	
}
