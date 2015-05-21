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
