package org.avuna.httpd.event;

public class Event {
	private final int eid;
	
	public Event(int eid) {
		this.eid = eid;
	}
	
	public final int getEID() {
		return eid;
	}
	
	private boolean canceled = false;
	
	public final boolean isCanceled() {
		return canceled;
	}
	
	public boolean canCancel() {
		return true;
	}
	
	public final void cancel() {
		if (canCancel()) canceled = true;
	}
}
