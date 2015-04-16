package org.avuna.httpd.mail.imap.command;

import java.io.IOException;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.imap.IMAPCommand;
import org.avuna.httpd.mail.imap.IMAPWork;
import org.avuna.httpd.mail.mailbox.Email;
import org.avuna.httpd.mail.mailbox.Mailbox;

public class IMAPCommandStatus extends IMAPCommand {
	
	public IMAPCommandStatus(String comm, int minState, int maxState, HostMail host) {
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
				int recent = 0;
				for (Email e : m.emails) {
					if (e.flags.contains("\\Recent")) recent++;
				}
				int unseen = 0;
				for (Email e : m.emails) {
					if (!e.flags.contains("\\Seen")) unseen++;
				}
				focus.writeLine(focus, "*", "STATUS " + m.name + " (MESSAGES " + m.emails.size() + " RECENT " + recent + " UIDNEXT " + (m.emails.size() + 1) + " UIDVALIDITY " + Integer.MAX_VALUE + " UNSEEN unseen)");
				focus.writeLine(focus, letters, "OK Status.");
			}
		}else {
			focus.writeLine(focus, letters, "BAD No mailbox.");
		}
	}
	
}
