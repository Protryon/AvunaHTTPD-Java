package org.avuna.httpd.http.event;

import org.avuna.httpd.event.Event;

public class EventPreExit extends Event {
	
	public EventPreExit() {
		super(HTTPEventID.PREEXIT);
	}
	
	public boolean canCancel() {
		return false;
	}
}
