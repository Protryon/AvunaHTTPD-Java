/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.mail.imap.command;

import java.io.IOException;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.imap.ICommandCallback;
import org.avuna.httpd.mail.imap.IMAPBlockStatus;
import org.avuna.httpd.mail.imap.IMAPCommand;
import org.avuna.httpd.mail.imap.IMAPWork;
import org.avuna.httpd.mail.mailbox.Email;
import org.avuna.httpd.mail.mailbox.Mailbox;
import org.avuna.httpd.mail.util.StringFormatter;

public class IMAPCommandAppend extends IMAPCommand implements ICommandCallback {
	
	public IMAPCommandAppend(String comm, int minState, int maxState, HostMail host) {
		super(comm, minState, maxState, host);
	}
	
	private static final class IMAPBlockStatusAppend extends IMAPBlockStatus {
		public String flags, letters;
		public Mailbox mb;
		
		public IMAPBlockStatusAppend(String flags, String letters, Mailbox mb) {
			this.flags = flags;
			this.mb = mb;
			this.letters = letters;
		}
	}
	
	@Override
	public void run(IMAPWork focus, String letters, String[] pa) throws IOException {
		String[] args = pa;
		if (args.length >= 2) {
			args = StringFormatter.congealBySurroundings(args, "(", ")");
			String mailbox = args[0];
			Mailbox mb = focus.authUser.getMailbox(mailbox);
			if (mb == null) {
				focus.writeLine(letters, "NO Mailbox doesn't exist.");
				return;
			}
			focus.writeLine("+", "Ready for literal data");
			String flags = "";
			if (args.length >= 3) {
				flags = args[1].substring(1, args[1].length() - 1);
			}
			@SuppressWarnings("unused")
			String date = "";
			if (args.length >= 4) {
				date = args[2];
			}
			String length = args[args.length - 1].substring(1, args[args.length - 1].length() - 1);
			int l = Integer.parseInt(length);
			if (host.unio()) {
				focus.requestBlock(this, l, new IMAPBlockStatusAppend(flags, letters, mb));
			}else {
				byte[] data = new byte[l];
				focus.in.readFully(data);
				receiveBlock(focus, data, new IMAPBlockStatusAppend(flags, letters, mb));
			}
		}else {
			focus.writeLine(letters, "BAD No mailbox.");
		}
	}
	
	@Override
	public void receiveBlock(IMAPWork focus, byte[] block, IMAPBlockStatus status) throws IOException {
		IMAPBlockStatusAppend sa = (IMAPBlockStatusAppend) status;
		String ed = new String(block);
		synchronized (sa.mb.emails) {
			Email eml = new Email(host, ed, sa.mb.emails.length + 1, focus.authUser.email);
			for (String flag : sa.flags.split(" ")) {
				eml.flags.add(flag);
			}
			host.logger.log(ed);
			Email[] ne = new Email[sa.mb.emails.length + 1];
			System.arraycopy(sa.mb.emails, 0, ne, 0, sa.mb.emails.length);
			ne[ne.length - 1] = eml;
			sa.mb.emails = ne;
		}
		focus.writeLine(sa.letters, "OK");
	}
	
}
