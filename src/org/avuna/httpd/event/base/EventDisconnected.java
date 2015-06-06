package org.avuna.httpd.event.base;

import org.avuna.httpd.event.Event;
import org.avuna.httpd.http.networking.Work;

public class EventDisconnected extends Event {
	private final Work work;
	
	public Work getWork() {
		return work;
	}
	
	public EventDisconnected(Work work) {
		super(EventID.DISCONNECTED);
		this.work = work;
	}
	
	public boolean canCancel() {
		return false;
	}
	
}
