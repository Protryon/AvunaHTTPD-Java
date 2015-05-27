package org.avuna.httpd.mail.imap.command;

import java.io.IOException;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.imap.IMAPCommand;
import org.avuna.httpd.mail.imap.IMAPWork;
import org.avuna.httpd.mail.mailbox.Email;
import org.avuna.httpd.mail.mailbox.Mailbox;
import org.avuna.httpd.mail.util.StringFormatter;
import org.avuna.httpd.util.Logger;

public class IMAPCommandAppend extends IMAPCommand {
	
	public IMAPCommandAppend(String comm, int minState, int maxState, HostMail host) {
		super(comm, minState, maxState, host);
	}
	
	@Override
	public void run(IMAPWork focus, String letters, String[] pa) throws IOException {
		String[] args = pa;
		if (args.length >= 2) {
			args = StringFormatter.congealBySurroundings(args, "(", ")");
			String mailbox = args[0];
			Mailbox mb = focus.authUser.getMailbox(mailbox);
			if (mb == null) {
				focus.writeLine(focus, letters, "NO Mailbox doesn't exist.");
				return;
			}
			focus.writeLine(focus, "+", "Ready for literal data");
			String flags = "";
			if (args.length >= 3) {
				flags = args[1].substring(1, args[1].length() - 1);
			}
			@SuppressWarnings("unused")
			String date = "";
			if (args.length >= 4) {
				date = args[2];
			}
			String length = args[args.length - 1].substring(1, args[args.length - 1].length() - 1);
			int l = Integer.parseInt(length);
			byte[] data = new byte[l];
			focus.in.readFully(data);
			String ed = new String(data);
			synchronized (mb.emails) {
				Email eml = new Email(ed, mb.emails.length + 1, focus.authUser.email);
				for (String flag : flags.split(" ")) {
					eml.flags.add(flag);
				}
				Logger.log(ed);
				Email[] ne = new Email[mb.emails.length + 1];
				System.arraycopy(mb.emails, 0, ne, 0, mb.emails.length);
				ne[ne.length - 1] = eml;
				mb.emails = ne;
			}
			focus.writeLine(focus, letters, "OK");
		}else {
			focus.writeLine(focus, letters, "BAD No mailbox.");
		}
	}
	
}
