package org.avuna.httpd.http.event;

import org.avuna.httpd.event.Event;

public class EventDisconnected extends Event {
	
	public EventDisconnected() {
		super(HTTPEventID.DISCONNECTED);
	}
	
}
