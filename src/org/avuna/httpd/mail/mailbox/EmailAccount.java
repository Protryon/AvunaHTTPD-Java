package org.avuna.httpd.mail.mailbox;

import java.util.ArrayList;

public class EmailAccount {
	public final String email;
	public final String password;
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
