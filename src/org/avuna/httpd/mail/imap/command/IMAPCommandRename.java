package org.avuna.httpd.mail.imap.command;

import java.io.IOException;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.imap.IMAPCommand;
import org.avuna.httpd.mail.imap.IMAPWork;
import org.avuna.httpd.mail.mailbox.Mailbox;

public class IMAPCommandRename extends IMAPCommand {
	
	public IMAPCommandRename(String comm, int minState, int maxState, HostMail host) {
		super(comm, minState, maxState, host);
	}
	
	@Override
	public void run(IMAPWork focus, String letters, String[] args) throws IOException {
		if (args.length >= 2) {
			String ms = args[0];
			if (ms.startsWith("\"")) {
				ms = ms.substring(1);
			}
			if (ms.endsWith("\"")) {
				ms = ms.substring(0, ms.length() - 1);
			}
			String nn = args[1];
			if (nn.startsWith("\"")) {
				nn = nn.substring(1);
			}
			if (nn.endsWith("\"")) {
				nn = nn.substring(0, ms.length() - 1);
			}
			Mailbox m = focus.authUser.getMailbox(ms);
			if (m == null || m.name.equals("INBOX") || m.name.equals("Trash")) {
				focus.writeLine(focus, letters, "NO Invalid Mailbox.");
			}else {
				m.name = nn;
				focus.writeLine(focus, letters, "OK Mailbox renamed.");
			}
		}else {
			focus.writeLine(focus, letters, "BAD No mailbox.");
		}
	}
	
}
