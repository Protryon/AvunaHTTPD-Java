package org.avuna.httpd.http.event;

import org.avuna.httpd.event.Event;
import org.avuna.httpd.http.networking.Work;

public class EventConnected extends Event {
	private final Work work;
	
	public Work getWork() {
		return work;
	}
	
	public EventConnected(Work work) {
		super(HTTPEventID.CONNECTED);
		this.work = work;
	}
	
}
