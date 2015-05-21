package org.avuna.httpd.mail.imap;

import java.io.IOException;
import org.avuna.httpd.hosts.HostMail;

public abstract class IMAPCommand {
	public final String comm;
	public final int minState, maxState;
	protected final HostMail host;
	
	public IMAPCommand(String comm, int minState, int maxState, HostMail host) {
		this.host = host;
		this.comm = comm;
		this.minState = minState;
		this.maxState = maxState;
	}
	
	public abstract void run(IMAPWork focus, String letters, String[] args) throws IOException;
}
