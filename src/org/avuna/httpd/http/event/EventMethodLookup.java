package org.avuna.httpd.http.event;

import org.avuna.httpd.event.Event;

public class EventMethodLookup extends Event {
	
	public EventMethodLookup() {
		super(HTTPEventID.METHODLOOKUP);
	}
	
}
