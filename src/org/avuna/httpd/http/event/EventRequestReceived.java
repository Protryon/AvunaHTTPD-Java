package org.avuna.httpd.http.event;

import org.avuna.httpd.event.Event;

public class EventRequestReceived extends Event {
	
	public EventRequestReceived() {
		super(HTTPEventID.REQUESTRECEIVED);
	}
	
}
