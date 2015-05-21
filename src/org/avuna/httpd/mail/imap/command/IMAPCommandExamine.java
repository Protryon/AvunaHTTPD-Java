package org.avuna.httpd.mail.imap.command;

import java.io.IOException;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.imap.IMAPCommand;
import org.avuna.httpd.mail.imap.IMAPWork;
import org.avuna.httpd.mail.mailbox.Email;
import org.avuna.httpd.mail.mailbox.Mailbox;

public class IMAPCommandExamine extends IMAPCommand {
	
	public IMAPCommandExamine(String comm, int minState, int maxState, HostMail host) {
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
			if (m != null) {
				if (focus.selectedMailbox != null) {
					focus.writeLine(focus, "*", "OK [CLOSED] Previous mailbox closed.");
				}
				focus.selectedMailbox = m;
				focus.isExamine = true;
				focus.state = 3;
				focus.writeLine(focus, "*", "FLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft)");
				focus.writeLine(focus, "*", "OK [PERMANENTFLAGS ()] Read-only mailbox.");
				focus.writeLine(focus, "*", m.emails.size() + " EXISTS");
				int recent = 0;
				for (Email e : m.emails) {
					if (e.flags.contains("\\Recent")) recent++;
				}
				focus.writeLine(focus, "*", recent + " RECENT");
				focus.writeLine(focus, "*", "OK [UIDVALIDITY " + Integer.MAX_VALUE + "] UIDs valid");
				focus.writeLine(focus, "*", "OK [UIDNEXT " + (m.emails.size() + 1) + "] Predicted next UID");
				focus.writeLine(focus, "*", "OK [HIGHESTMODSEQ 1] Highest");
				focus.writeLine(focus, letters, "OK [READ-ONLY] Select completed.");
			}else {
				focus.writeLine(focus, letters, "NO Invalid mailbox.");
			}
		}else {
			focus.writeLine(focus, letters, "BAD No mailbox.");
		}
	}
	
}
