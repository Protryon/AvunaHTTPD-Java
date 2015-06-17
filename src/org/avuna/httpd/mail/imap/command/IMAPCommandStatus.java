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
import org.avuna.httpd.mail.util.StringFormatter;

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
				String match = "MESSAGES RECENT UIDNEXT UIDVALIDITY UNSEEN";
				String[] ca = StringFormatter.congealBySurroundings(args, "(", ")");
				if (ca.length == 2) {
					match = ca[1].trim();
					if (match.startsWith("(")) {
						match = match.substring(1, match.length() - 1);
					}
				}
				synchronized (m.emails) {
					StringBuilder ret = new StringBuilder();
					ret.append("STATUS ").append(m.name).append(" (");
					for (String s : match.split(" ")) {
						ret.append(s).append(" ");
						String s2 = s.toLowerCase().trim();
						if (s2.equals("messages")) {
							ret.append(m.emails.length);
						}else if (s2.equals("recent")) {
							int recent = 0;
							ret.append(recent);
							for (Email e : m.emails) {
								if (e == null) continue;
								if (e.flags.contains("\\Recent")) recent++;
							}
						}else if (s2.equals("uidnext")) {
							ret.append(m.emails.length + 1);
						}else if (s2.equals("uidvalidity")) {
							ret.append(Integer.MAX_VALUE);
						}else if (s2.equals("unseen")) {
							int unseen = 0;
							for (Email e : m.emails) {
								if (e == null) continue;
								if (!e.flags.contains("\\Seen")) unseen++;
							}
							ret.append(unseen);
						}
						ret.append(" ");
					}
					IMAPCommandFetch.trim(ret);
					ret.append(")");
					focus.writeLine(focus, "*", ret.toString());
				}
				focus.writeLine(focus, letters, "OK Status.");
			}
		}else {
			focus.writeLine(focus, letters, "BAD No mailbox.");
		}
	}
}
