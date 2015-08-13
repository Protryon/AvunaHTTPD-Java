/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

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
				focus.writeLine("*", "LIST (\\Noselect) \"/\" \"/\"");
				focus.writeLine(letters, "OK Mailbox list.");
			}else if (mn.equals("%") || mn.equals("*")) {
				for (Mailbox m : focus.authUser.mailboxes) {
					if (!m.subscribed) {
						m = null;
					}
					if (m != null) {
						String ef = null;
						if (m.name.equals("Trash")) ef = "\\Trash";
						else if (m.name.equals("Drafts")) ef = "\\Drafts";
						else if (m.name.equals("Sent")) ef = "\\Sent";
						focus.writeLine("*", "LIST (\\HasNoChildren" + (ef == null ? "" : " " + ef) + ") \"/\" \"" + m.name + "\"");
					}
				}
				focus.writeLine("*", "LIST (\\Noselect \\HasChildren) \"/\" \"[Avuna Mail]\"");
				focus.writeLine(letters, "OK Mailbox list.");
			}else {
				Mailbox m = mn.length() == 0 && focus.selectedMailbox != null ? focus.selectedMailbox : focus.authUser.getMailbox(mn);
				if (m == null) {
					focus.writeLine(letters, "NO Invalid Mailbox.");
				}else {
					String ef = null;
					if (m.name.equals("Trash")) ef = "\\Trash";
					else if (m.name.equals("Drafts")) ef = "\\Drafts";
					else if (m.name.equals("Sent")) ef = "\\Sent";
					focus.writeLine("*", "LIST (\\HasNoChildren" + (ef == null ? "" : " " + ef) + ") \"/\" \"" + m.name + "\"");
					focus.writeLine(letters, "OK Mailbox list.");
				}
			}
		}else {
			focus.writeLine(letters, "BAD No mailbox.");
		}
	}
	
}
