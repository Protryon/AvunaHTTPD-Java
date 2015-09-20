/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

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
						if (i < emails.length && i >= 0) {
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
