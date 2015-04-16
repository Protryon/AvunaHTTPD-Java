package org.avuna.httpd.mail.imap.command;

import java.io.IOException;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.imap.IMAPCommand;
import org.avuna.httpd.mail.imap.IMAPWork;

public class IMAPCommandSearch extends IMAPCommand {
	
	public IMAPCommandSearch(String comm, int minState, int maxState, HostMail host) {
		super(comm, minState, maxState, host);
	}
	
	@Override
	public void run(IMAPWork focus, String letters, String[] args) throws IOException {
		if (args.length >= 2) {
			args = new String[]{args[1]};
		}
		if (args.length >= 1) {
			focus.writeLine(focus, "*", "SEARCH");
		}
		focus.writeLine(focus, letters, "OK Not yet implemented.");
	}
	
}
