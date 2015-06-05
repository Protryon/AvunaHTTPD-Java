package org.avuna.httpd.http.event;

import org.avuna.httpd.event.Event;

public class EventPostInit extends Event {
	
	public EventPostInit() {
		super(HTTPEventID.POSTINIT);
	}
	
	public boolean canCancel() {
		return false;
	}
}
