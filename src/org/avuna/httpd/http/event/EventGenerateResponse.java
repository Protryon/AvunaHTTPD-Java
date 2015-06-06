package org.avuna.httpd.http.event;

import org.avuna.httpd.event.Event;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;

public class EventGenerateResponse extends Event {
	private final RequestPacket request;
	private final ResponsePacket response;
	
	public RequestPacket getRequest() {
		return request;
	}
	
	public ResponsePacket getResponse() {
		return response;
	}
	
	public EventGenerateResponse(RequestPacket request, ResponsePacket response) {
		super(HTTPEventID.GENERATERESPONSE);
		this.request = request;
		this.response = response;
	}
	
}
