package org.avuna.httpd.mail.imap.command;

import java.io.IOException;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.imap.IMAPCommand;
import org.avuna.httpd.mail.imap.IMAPWork;

public class IMAPCommandAppend extends IMAPCommand {
	
	public IMAPCommandAppend(String comm, int minState, int maxState, HostMail host) {
		super(comm, minState, maxState, host);
	}
	
	@Override
	public void run(IMAPWork focus, String letters, String[] args) throws IOException {
		if (args.length >= 2) {
			focus.writeLine(focus, letters, "BAD Not Implemented.");
		}else {
			focus.writeLine(focus, letters, "BAD No mailbox.");
		}
	}
	
}
