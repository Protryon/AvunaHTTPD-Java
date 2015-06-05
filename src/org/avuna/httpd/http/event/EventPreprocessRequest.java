package org.avuna.httpd.http.event;

import org.avuna.httpd.event.Event;
import org.avuna.httpd.http.networking.RequestPacket;

public class EventPreprocessRequest extends Event {
	private final RequestPacket request;
	
	public RequestPacket getRequest() {
		return request;
	}
	
	public EventPreprocessRequest(RequestPacket request) {
		super(HTTPEventID.PREPROCESSREQUEST);
		this.request = request;
	}
	
}
