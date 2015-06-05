package org.avuna.httpd.event;

public class Event {
	private final int eid;
	
	public Event(int eid) {
		this.eid = eid;
	}
	
	public int getEID() {
		return eid;
	}
	
	private boolean canceled = false;
	
	public boolean isCanceled() {
		return canceled;
	}
	
	public void cancel() {
		canceled = true;
	}
}
