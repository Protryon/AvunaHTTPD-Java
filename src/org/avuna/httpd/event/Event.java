package org.avuna.httpd.event;

public class Event {
	private final int eid;
	
	public Event(int eid) {
		this.eid = eid;
	}
	
	public int getEID() {
		return eid;
	}
}
