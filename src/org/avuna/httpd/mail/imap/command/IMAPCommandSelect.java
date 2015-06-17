/*	Avuna HTTPD - General Server Applications
    Copyright (C) 2015 Maxwell Bruce

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package org.avuna.httpd.mail.imap.command;

import java.io.IOException;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.imap.IMAPCommand;
import org.avuna.httpd.mail.imap.IMAPWork;
import org.avuna.httpd.mail.mailbox.Email;
import org.avuna.httpd.mail.mailbox.Mailbox;

public class IMAPCommandSelect extends IMAPCommand {
	
	public IMAPCommandSelect(String comm, int minState, int maxState, HostMail host) {
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
				focus.state = 3;
				focus.writeLine(focus, "*", "FLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft)");
				focus.writeLine(focus, "*", "OK [PERMANENTFLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft \\*)] Flags permitted.");
				int recent = 0, unseen = 0;
				synchronized (m.emails) {
					focus.writeLine(focus, "*", m.emails.length + " EXISTS");
					for (Email e : m.emails) {
						if (e != null && e.flags.contains("\\Recent")) recent++;
					}
					for (Email e : m.emails) {
						if (e != null && !e.flags.contains("\\Seen")) unseen++;
					}
				}
				focus.writeLine(focus, "*", recent + " RECENT");
				focus.writeLine(focus, "*", "OK [UNSEEN " + unseen + "] Unseen messages");
				focus.writeLine(focus, "*", "OK [UIDVALIDITY " + Integer.MAX_VALUE + "] UIDs valid");
				synchronized (m.emails) {
					focus.writeLine(focus, "*", "OK [UIDNEXT " + (m.emails.length + 1) + "] Predicted next UID");
				}
				focus.writeLine(focus, "*", "OK [HIGHESTMODSEQ 1] Highest");
				focus.writeLine(focus, letters, "OK [READ-WRITE] Select completed.");
			}else {
				focus.writeLine(focus, letters, "NO Invalid mailbox.");
			}
		}else {
			focus.writeLine(focus, letters, "BAD No mailbox.");
		}
	}
	
}
