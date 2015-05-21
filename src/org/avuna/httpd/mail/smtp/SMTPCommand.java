package org.avuna.httpd.mail.smtp;

import java.io.IOException;

public abstract class SMTPCommand {
	public final String comm;
	public final int minState, maxState;
	
	public SMTPCommand(String comm, int minState, int maxState) {
		this.comm = comm;
		this.minState = minState;
		this.maxState = maxState;
	}
	
	public abstract void run(SMTPWork focus, String line) throws IOException;
}
