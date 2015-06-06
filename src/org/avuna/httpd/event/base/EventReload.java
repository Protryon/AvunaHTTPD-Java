package org.avuna.httpd.event.base;

import org.avuna.httpd.event.Event;

public class EventReload extends Event {
	
	public EventReload() {
		super(EventID.RELOAD);
	}
	
}
