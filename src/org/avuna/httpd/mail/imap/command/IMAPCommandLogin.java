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
import org.avuna.httpd.mail.mailbox.EmailAccount;
import org.avuna.httpd.mail.util.StringFormatter;

public class IMAPCommandLogin extends IMAPCommand {
	
	public IMAPCommandLogin(String comm, int minState, int maxState, HostMail host) {
		super(comm, minState, maxState, host);
	}
	
	@Override
	public void run(IMAPWork focus, String letters, String[] args) throws IOException {
		String[] cargs = StringFormatter.congealBySurroundings(args, "\"", "\"");
		for (int i = 0; i < cargs.length; i++) {
			if (cargs[i].startsWith("\"") && cargs[i].endsWith("\"")) {
				cargs[i] = cargs[i].substring(1, cargs[i].length() - 1);
			}
		}
		if (cargs.length == 2) {
			EmailAccount us = null;
			for (EmailAccount e : host.accounts) {
				if (e.email.equals(cargs[0]) && e.password.equals(cargs[1])) {
					us = e;
					break;
				}
			}
			if (us != null) {
				focus.writeLine(focus, letters, "OK");
				focus.authUser = us;
				focus.state = 2;
			}else {
				focus.writeLine(focus, letters, "NO Authenticate Failed.");
				focus.state = 0;
			}
		}else {
			focus.writeLine(focus, letters, "BAD " + cargs.length + " arguments, not 2.");
		}
	}
	
}
