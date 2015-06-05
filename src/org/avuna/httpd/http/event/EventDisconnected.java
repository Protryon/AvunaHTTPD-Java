package org.avuna.httpd.http.event;

import org.avuna.httpd.event.Event;
import org.avuna.httpd.http.networking.Work;

public class EventDisconnected extends Event {
	private final Work work;
	
	public Work getWork() {
		return work;
	}
	
	public EventDisconnected(Work work) {
		super(HTTPEventID.DISCONNECTED);
		this.work = work;
	}
	
}
