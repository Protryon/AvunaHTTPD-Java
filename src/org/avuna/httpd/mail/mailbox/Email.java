package org.avuna.httpd.mail.mailbox;

import java.util.ArrayList;

public class Email {
	public final ArrayList<String> flags = new ArrayList<String>();
	public final String data, from;
	public final ArrayList<String> to = new ArrayList<String>();
	public int uid;
	
	public Email(String data, int uid, String from) {
		this.data = data;
		this.uid = uid;
		this.from = from;
	}
}
