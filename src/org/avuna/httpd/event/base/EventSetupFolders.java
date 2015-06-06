package org.avuna.httpd.event.base;

import org.avuna.httpd.event.Event;

public class EventSetupFolders extends Event {
	
	public EventSetupFolders() {
		super(EventID.SETUPFOLDERS);
	}
	
	public boolean canCancel() {
		return false;
	}
	
}
