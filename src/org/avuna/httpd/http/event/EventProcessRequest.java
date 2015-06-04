package org.avuna.httpd.http.event;

import org.avuna.httpd.event.Event;

public class EventProcessRequest extends Event {
	
	public EventProcessRequest() {
		super(HTTPEventID.PROCESSREQUEST);
	}
	
}
