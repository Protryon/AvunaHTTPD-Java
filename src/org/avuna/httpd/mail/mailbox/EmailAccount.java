/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.mail.mailbox;

import java.util.ArrayList;

public class EmailAccount {
	public final String email;
	public String password;
	public final ArrayList<Mailbox> mailboxes = new ArrayList<Mailbox>();
	public final Mailbox INBOX;
	
	public EmailAccount(String email, String password) {
		this.email = email;
		this.password = password;
		mailboxes.add(INBOX = new Mailbox(this, "INBOX"));
		mailboxes.add(new Mailbox(this, "Trash"));
		mailboxes.add(new Mailbox(this, "Sent"));
		mailboxes.add(new Mailbox(this, "Drafts"));
	}
	
	public Mailbox getMailbox(String name) {
		String name2 = name;
		if (name2.startsWith("\"")) name2 = name2.substring(1, name2.length() - 1);
		for (Mailbox m : mailboxes) {
			if (m.name.equalsIgnoreCase(name2)) {
				return m;
			}
		}
		return null;
	}
	
	public void deliver(Email email) {
		email.flags.add("\\Recent");
		email.flags.add("\\Unseen");
		synchronized (INBOX.emails) {
			email.uid = INBOX.emails.length + 1;
			Email[] ne = new Email[INBOX.emails.length + 1];
			System.arraycopy(INBOX.emails, 0, ne, 0, INBOX.emails.length);
			ne[ne.length - 1] = email;
			INBOX.emails = ne;
		}
	}
}
