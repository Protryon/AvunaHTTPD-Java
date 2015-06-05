package org.avuna.httpd.http.event;

import org.avuna.httpd.event.Event;

public class EventReload extends Event {
	
	public EventReload() {
		super(HTTPEventID.RELOAD);
	}
	
}
