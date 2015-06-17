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
