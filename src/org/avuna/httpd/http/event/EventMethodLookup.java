package org.avuna.httpd.http.event;

import org.avuna.httpd.event.Event;
import org.avuna.httpd.http.networking.RequestPacket;

/**
 * You MUST call cancel if you handle the method.
 */
public class EventMethodLookup extends Event {
	
	private final String method;
	private final RequestPacket request;
	
	public String getMethod() {
		return method;
	}
	
	public RequestPacket getRequest() {
		return request;
	}
	
	public EventMethodLookup(String method, RequestPacket request) {
		super(HTTPEventID.METHODLOOKUP);
		this.method = method;
		this.request = request;
	}
	
}
