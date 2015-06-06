package org.avuna.httpd.event.base;

import org.avuna.httpd.event.Event;
import org.avuna.httpd.http.networking.Work;

public class EventConnected extends Event {
	private final Work work;
	
	public Work getWork() {
		return work;
	}
	
	public EventConnected(Work work) {
		super(EventID.CONNECTED);
		this.work = work;
	}
	
}
