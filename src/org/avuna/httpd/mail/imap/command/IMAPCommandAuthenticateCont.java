package org.avuna.httpd.mail.imap.command;

import java.io.IOException;
import javax.xml.bind.DatatypeConverter;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.imap.IMAPCommand;
import org.avuna.httpd.mail.imap.IMAPWork;
import org.avuna.httpd.mail.mailbox.EmailAccount;

public class IMAPCommandAuthenticateCont extends IMAPCommand {
	
	public IMAPCommandAuthenticateCont(String comm, int minState, int maxState, HostMail host) {
		super(comm, minState, maxState, host);
	}
	
	@Override
	public void run(IMAPWork focus, String letters, String[] args) throws IOException {
		String up = new String(DatatypeConverter.parseBase64Binary(letters)).substring(1);
		String username = up.substring(0, up.indexOf(new String(new byte[]{0})));
		String password = up.substring(username.length() + 1);
		String letters2 = focus.authMethod.substring(5);
		// System.out.println(username + ":" + password);
		EmailAccount us = null;
		for (EmailAccount e : host.accounts) {
			if (e.email.equals(username) && e.password.equals(password)) {
				us = e;
				break;
			}
		}
		if (us != null) {
			focus.writeLine(focus, letters2, "OK");
			focus.authUser = us;
			focus.state = 2;
		}else {
			focus.writeLine(focus, letters2, "NO Authenticate Failed.");
			focus.state = 0;
		}
	}
	
}
