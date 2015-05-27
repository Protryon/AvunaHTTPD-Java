package org.avuna.httpd.mail.imap.command;

import java.io.IOException;
import java.util.ArrayList;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.imap.IMAPCommand;
import org.avuna.httpd.mail.imap.IMAPWork;
import org.avuna.httpd.mail.mailbox.Email;
import org.avuna.httpd.mail.mailbox.Mailbox;

public class IMAPCommandCopy extends IMAPCommand {
	
	public IMAPCommandCopy(String comm, int minState, int maxState, HostMail host) {
		super(comm, minState, maxState, host);
	}
	
	@Override
	public void run(IMAPWork focus, String letters, String[] args) throws IOException {
		if (args.length != 2) {
			focus.writeLine(focus, letters, "BAD Invalid Arguments.");
			return;
		}
		Mailbox tct = focus.authUser.getMailbox(args[1]);
		if (tct == null) {
			focus.writeLine(focus, letters, "NO [TRYCREATE] Mailbox doesn't exist!");
			return;
		}
		ArrayList<Email> tc = focus.selectedMailbox.getByIdentifier(args[0]);
		synchronized (tct.emails) {
			Email[] ne = new Email[tct.emails.length + tc.size()];
			System.arraycopy(tct.emails, 0, ne, 0, tct.emails.length);
			System.arraycopy(tc.toArray(new Email[0]), 0, ne, tct.emails.length, tc.size());
			tct.emails = ne;
		}
		focus.writeLine(focus, letters, "OK Copied.");
	}
	
}
