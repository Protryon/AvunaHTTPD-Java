package org.avuna.httpd.mail.imap.command;

import java.io.IOException;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.imap.IMAPCommand;
import org.avuna.httpd.mail.imap.IMAPWork;

public class IMAPCommandUID extends IMAPCommand {
	private final IMAPCommandFetch fetch;
	private final IMAPCommandStore store;
	private final IMAPCommandSearch search;
	private final IMAPCommandCopy copy;
	
	public IMAPCommandUID(String comm, int minState, int maxState, HostMail host, IMAPCommandFetch fetch, IMAPCommandStore store, IMAPCommandSearch search, IMAPCommandCopy copy) {
		super(comm, minState, maxState, host);
		this.fetch = fetch;
		this.store = store;
		this.search = search;
		this.copy = copy;
	}
	
	@Override
	public void run(IMAPWork focus, String letters, String[] args) throws IOException {
		if (args.length >= 1) {
			if (args[0].toLowerCase().equals("fetch")) {
				String[] nargs = new String[args.length - 1];
				for (int i = 0; i < nargs.length; i++) {
					nargs[i] = args[i + 1];
				}
				if (nargs.length >= 2 && nargs[1].startsWith("(") && !nargs[1].contains("UID")) {
					nargs[1] = "(UID " + nargs[1].substring(1);
				}
				fetch.run(focus, letters, nargs);
			}else if (args[0].toLowerCase().equals("store")) {
				String[] nargs = new String[args.length - 1];
				for (int i = 0; i < nargs.length; i++) {
					nargs[i] = args[i + 1];
				}
				store.run(focus, letters, nargs);
			}else if (args[0].toLowerCase().equals("search")) {
				String[] nargs = new String[args.length - 1];
				for (int i = 0; i < nargs.length; i++) {
					nargs[i] = args[i + 1];
				}
				search.run(focus, letters, nargs);
			}else if (args[0].toLowerCase().equals("copy")) {
				String[] nargs = new String[args.length - 1];
				for (int i = 0; i < nargs.length; i++) {
					nargs[i] = args[i + 1];
				}
				copy.run(focus, letters, nargs);
			}else {
				focus.writeLine(focus, letters, "BAD Missing Arguments.");
			}
		}else {
			focus.writeLine(focus, letters, "BAD Missing Arguments.");
		}
	}
	
}
