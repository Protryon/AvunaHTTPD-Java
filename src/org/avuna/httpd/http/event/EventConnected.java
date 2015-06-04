package org.avuna.httpd.http.event;

import org.avuna.httpd.event.Event;

public class EventConnected extends Event {
	
	public EventConnected() {
		super(HTTPEventID.CONNECTED);
	}
	
}
