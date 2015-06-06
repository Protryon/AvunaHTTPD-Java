package org.avuna.httpd.event.base;

import org.avuna.httpd.event.Event;

public class EventPostInit extends Event {
	
	public EventPostInit() {
		super(EventID.POSTINIT);
	}
	
	public boolean canCancel() {
		return false;
	}
}
