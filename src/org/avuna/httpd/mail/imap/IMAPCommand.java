package org.avuna.httpd.mail.imap;

import java.io.IOException;

public abstract class IMAPCommand {
	public final String comm;
	public final int minState, maxState;
	
	public IMAPCommand(String comm, int minState, int maxState) {
		this.comm = comm;
		this.minState = minState;
		this.maxState = maxState;
	}
	
	public abstract void run(IMAPWork focus, String letters, String[] args) throws IOException;
}
