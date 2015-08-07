/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.mail.imap.command;

import java.io.IOException;
import java.util.ArrayList;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.imap.IMAPCommand;
import org.avuna.httpd.mail.imap.IMAPWork;
import org.avuna.httpd.mail.mailbox.Email;

public class IMAPCommandStore extends IMAPCommand {
	
	public IMAPCommandStore(String comm, int minState, int maxState, HostMail host) {
		super(comm, minState, maxState, host);
	}
	
	@Override
	public void run(IMAPWork focus, String letters, String[] args) throws IOException {
		if (args.length >= 3) {
			ArrayList<Email> toFetch = focus.selectedMailbox.getByIdentifier(args[0]);
			String fc = args[1].toLowerCase();
			String[] flags = args[2].substring(1, args[2].length() - 1).split(" ");
			for (Email e : toFetch) {
				if (fc.startsWith("+")) {
					for (String flag : flags) {
						if (flag.equals("\\Seen")) {
							e.flags.remove("\\Unseen");
						}else if (flag.equals("\\Unseen")) {
							e.flags.remove("\\Seen");
						}
						if (flag.equals("\\Deleted")) {
							focus.selectedMailbox.emails[e.uid - 1] = null;
						}else if (!e.flags.contains(flag)) e.flags.add(flag);
					}
				}else if (fc.startsWith("-")) {
					for (String flag : flags) {
						if (e.flags.contains(flag)) {
							e.flags.remove(flag);
						}
					}
				}else {
					boolean recent = e.flags.contains("\\Recent");
					e.flags.clear();
					if (recent) e.flags.add("\\Recent");
					for (String flag : flags) {
						e.flags.add(flag);
					}
				}
				if (!fc.toLowerCase().endsWith(".silent")) {
					String ret = e.uid + " FETCH (FLAGS (";
					for (String flag : e.flags) {
						ret += flag + " ";
					}
					ret = ret.trim();
					ret += "))";
					focus.writeLine(focus, "*", ret);
				}
			}
			focus.writeLine(focus, letters, "OK");
		}else {
			focus.writeLine(focus, letters, "BAD Missing Arguments.");
		}
	}
	
}
