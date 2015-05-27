package org.avuna.httpd.mail.imap.command;

import java.io.IOException;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.imap.IMAPCommand;
import org.avuna.httpd.mail.imap.IMAPWork;
import org.avuna.httpd.mail.mailbox.Mailbox;

public class IMAPCommandList extends IMAPCommand {
	
	public IMAPCommandList(String comm, int minState, int maxState, HostMail host) {
		super(comm, minState, maxState, host);
	}
	
	@Override
	public void run(IMAPWork focus, String letters, String[] args) throws IOException {
		if (args.length >= 2) {
			String rn = args[0];
			if (rn.startsWith("\"")) {
				rn = rn.substring(1);
			}
			if (rn.endsWith("\"")) {
				rn = rn.substring(0, rn.length() - 1);
			}
			String mn = args[1];
			if (mn.startsWith("\"")) {
				mn = mn.substring(1);
			}
			if (mn.endsWith("\"")) {
				mn = mn.substring(0, mn.length() - 1);
			}
			if (mn.equals("")) {
				focus.writeLine(focus, "*", "LIST (\\Noselect) \"/\" \"/\"");
				focus.writeLine(focus, letters, "OK Mailbox list.");
			}else if (mn.equals("%")) {
				for (Mailbox m : focus.authUser.mailboxes) {
					if (!m.subscribed) {
						m = null;
					}
					if (m != null) {
						focus.writeLine(focus, "*", "LIST (\\HasNoChildren) \"/\" \"" + m.name + "\"");
					}
				}
				focus.writeLine(focus, "*", "LIST (\\Noselect \\HasChildren) \"/\" \"[Avuna Mail]\"");
				focus.writeLine(focus, letters, "OK Mailbox list.");
			}else {
				Mailbox m = mn.length() == 0 && focus.selectedMailbox != null ? focus.selectedMailbox : focus.authUser.getMailbox(mn);
				if (m == null) {
					focus.writeLine(focus, letters, "NO Invalid Mailbox.");
				}else {
					focus.writeLine(focus, "*", "LIST (\\HasNoChildren) \"/\" \"" + m.name + "\"");
					focus.writeLine(focus, letters, "OK Mailbox list.");
				}
			}
		}else {
			focus.writeLine(focus, letters, "BAD No mailbox.");
		}
	}
	
}
