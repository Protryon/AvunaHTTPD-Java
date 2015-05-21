package org.avuna.httpd.mail.mailbox;

import java.util.ArrayList;

public class Mailbox {
	public final EmailAccount owner;
	public String name = "";
	public final ArrayList<Email> emails = new ArrayList<Email>();
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
				int f2 = f.equals("*") ? emails.size() : Integer.parseInt(f) - 1;
				for (; i < f2; i++) {
					toFetch.add(emails.get(i));
				}
			}else {
				if (seq.equals("*")) {
					toFetch.add(emails.get(emails.size() - 1));
				}else {
					toFetch.add(emails.get(Integer.parseInt(seq) - 1));
				}
			}
		}
		return toFetch;
	}
}
