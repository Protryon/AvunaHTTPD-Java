package org.avuna.httpd.mail.imap.command;

import java.io.IOException;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.imap.IMAPCommand;
import org.avuna.httpd.mail.imap.IMAPWork;

public class IMAPCommandClose extends IMAPCommand {
	public IMAPCommandClose(String comm, int minState, int maxState, HostMail host) {
		super(comm, minState, maxState, host);
	}
	
	@Override
	public void run(IMAPWork focus, String letters, String[] args) throws IOException {
		for (int i = 0; i < focus.selectedMailbox.emails.size(); i++) {
			if (focus.selectedMailbox.emails.get(i).flags.contains("\\Deleted")) {
				focus.selectedMailbox.emails.remove(i);
				i--;
			}
		}
		focus.state = 2;
		focus.selectedMailbox = null;
		focus.writeLine(focus, letters, "OK close.");
	}
	
}
