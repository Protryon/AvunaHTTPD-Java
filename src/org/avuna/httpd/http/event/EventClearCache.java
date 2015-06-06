package org.avuna.httpd.http.event;

import org.avuna.httpd.event.Event;

public class EventClearCache extends Event {
	
	public EventClearCache() {
		super(HTTPEventID.CLEARCACHE);
	}
	
}
