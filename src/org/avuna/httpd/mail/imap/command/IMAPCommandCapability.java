package org.avuna.httpd.mail.imap.command;

import java.io.IOException;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.imap.IMAPCommand;
import org.avuna.httpd.mail.imap.IMAPWork;

public class IMAPCommandCapability extends IMAPCommand {
	
	public IMAPCommandCapability(String comm, int minState, int maxState, HostMail host) {
		super(comm, minState, maxState, host);
	}
	
	@Override
	public void run(IMAPWork focus, String letters, String[] args) throws IOException {
		focus.writeLine(focus, "*", "CAPABILITY IMAP4rev1 AUTH=PLAIN AUTH=LOGIN STARTTLS");
		focus.writeLine(focus, letters, "OK Capability completed.");
	}
	
}
