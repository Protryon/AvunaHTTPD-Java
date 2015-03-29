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
	}
	
	public Mailbox getMailbox(String name) {
		for (Mailbox m : mailboxes) {
			if (m.name.equals(name)) {
				return m;
			}
		}
		return null;
	}
	
	public void deliver(Email email) {
		email.uid = INBOX.emails.size() + 1;
		email.flags.add("\\Recent");
		email.flags.add("\\Unseen");
		INBOX.emails.add(email);
	}
	
}
