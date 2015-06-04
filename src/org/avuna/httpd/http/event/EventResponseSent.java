package org.avuna.httpd.http.event;

import org.avuna.httpd.event.Event;

public class EventResponseSent extends Event {
	
	public EventResponseSent() {
		super(HTTPEventID.RESPONSESENT);
	}
	
}
