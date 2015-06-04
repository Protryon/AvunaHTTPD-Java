package org.avuna.httpd.http.event;

import org.avuna.httpd.event.Event;

public class EventResponseFinished extends Event {
	
	public EventResponseFinished() {
		super(HTTPEventID.RESPONSEFINISHED);
	}
	
}
