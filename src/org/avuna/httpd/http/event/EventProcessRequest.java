package org.avuna.httpd.http.event;

import org.avuna.httpd.event.Event;
import org.avuna.httpd.http.networking.RequestPacket;

public class EventProcessRequest extends Event {
	private final RequestPacket request;
	
	public RequestPacket getRequst() {
		return request;
	}
	
	public EventProcessRequest(RequestPacket request) {
		super(HTTPEventID.PROCESSREQUEST);
		this.request = request;
	}
	
}
