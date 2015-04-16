package org.avuna.httpd.mail.imap.command;

import java.io.IOException;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.imap.IMAPCommand;
import org.avuna.httpd.mail.imap.IMAPWork;

public class IMAPCommandAuthenticate extends IMAPCommand {
	
	public IMAPCommandAuthenticate(String comm, int minState, int maxState, HostMail host) {
		super(comm, minState, maxState, host);
	}
	
	@Override
	public void run(IMAPWork focus, String letters, String[] args) throws IOException {
		if (args.length >= 1 && args[0].equalsIgnoreCase("plain")) {
			focus.writeLine(focus, "", "+");
			focus.state = 1;
			focus.authMethod = "plain" + letters;
		}else {
			focus.writeLine(focus, letters, "BAD No type.");
		}
	}
	
}
