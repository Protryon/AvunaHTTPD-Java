package org.avuna.httpd.event.base;

import org.avuna.httpd.event.Event;

public class EventPreExit extends Event {
	
	public EventPreExit() {
		super(EventID.PREEXIT);
	}
	
	public boolean canCancel() {
		return false;
	}
}
