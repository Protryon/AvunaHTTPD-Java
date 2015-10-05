/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.mail.imap.command;

import java.io.IOException;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.imap.IMAPCommand;
import org.avuna.httpd.mail.imap.IMAPWork;

public class IMAPCommandExpunge extends IMAPCommand {
	
	public IMAPCommandExpunge(String comm, int minState, int maxState, HostMail host) {
		super(comm, minState, maxState, host);
	}
	
	@Override
	public void run(IMAPWork focus, String letters, String[] args) throws IOException {
		synchronized (focus.selectedMailbox.emails) {
			for (int i = 0; i < focus.selectedMailbox.emails.length; i++) {
				if (focus.selectedMailbox.emails[i] != null && focus.selectedMailbox.emails[i].flags.contains("\\Deleted")) {
					focus.selectedMailbox.emails[i] = null;
					focus.writeLine("*", (i + 1) + " EXPUNGE");
					i--;
				}
			}
		}
		focus.writeLine(letters, "OK expunge.");
	}
	
}
