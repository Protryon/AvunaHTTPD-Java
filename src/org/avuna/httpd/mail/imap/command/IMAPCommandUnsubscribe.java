package org.avuna.httpd.mail.imap.command;

import java.io.IOException;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.imap.IMAPCommand;
import org.avuna.httpd.mail.imap.IMAPWork;
import org.avuna.httpd.mail.mailbox.Mailbox;

public class IMAPCommandUnsubscribe extends IMAPCommand {
	
	public IMAPCommandUnsubscribe(String comm, int minState, int maxState, HostMail host) {
		super(comm, minState, maxState, host);
	}
	
	@Override
	public void run(IMAPWork focus, String letters, String[] args) throws IOException {
		if (args.length >= 1) {
			String ms = args[0];
			if (ms.startsWith("\"")) {
				ms = ms.substring(1);
			}
			if (ms.endsWith("\"")) {
				ms = ms.substring(0, ms.length() - 1);
			}
			Mailbox m = focus.authUser.getMailbox(ms);
			if (m == null) {
				focus.writeLine(focus, letters, "NO Invalid Mailbox.");
			}else {
				m.subscribed = false;
				focus.writeLine(focus, letters, "OK Mailbox unsubscribed.");
			}
		}else {
			focus.writeLine(focus, letters, "BAD No mailbox.");
		}
	}
	
}
