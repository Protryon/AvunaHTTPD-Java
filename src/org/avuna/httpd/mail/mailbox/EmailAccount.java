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
		if (name.startsWith("\"")) name = name.substring(1, name.length() - 1);
		for (Mailbox m : mailboxes) {
			if (m.name.equalsIgnoreCase(name)) {
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
