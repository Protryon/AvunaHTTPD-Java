package org.avuna.httpd.mail.mailbox;

import java.util.ArrayList;

public class Mailbox {
	public final EmailAccount owner;
	public String name = "";
	public Email[] emails = new Email[0];
	public boolean subscribed = true;
	
	public Mailbox(EmailAccount owner, String name) {
		this.owner = owner;
		this.name = name;
	}
	
	public ArrayList<Email> getByIdentifier(String ids) {
		String[] seqs = ids.split(",");
		ArrayList<Email> toFetch = new ArrayList<Email>();
		for (String seq : seqs) {
			if (seq.contains(":")) {
				int i = Integer.parseInt(seq.substring(0, seq.indexOf(":"))) - 1;
				String f = seq.substring(seq.indexOf(":") + 1);
				synchronized (emails) {
					int f2 = f.equals("*") ? emails.length : Integer.parseInt(f) - 1;
					for (; i < f2; i++) {
						if (i < emails.length) {
							Email eml = emails[i];
							if (eml != null) toFetch.add(emails[i]);
						}
					}
				}
			}else {
				synchronized (emails) {
					if (seq.equals("*")) {
						Email eml = emails[emails.length - 1];
						if (eml != null) toFetch.add(eml);
					}else {
						int i = Integer.parseInt(seq) - 1;
						if (i < emails.length) {
							Email eml = emails[i];
							if (eml != null) toFetch.add(eml);
						}
					}
				}
			}
		}
		return toFetch;
	}
}
