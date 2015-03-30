package org.avuna.httpd.mail.imap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.mail.mailbox.EmailAccount;
import org.avuna.httpd.mail.mailbox.Mailbox;

public class IMAPWork {
	public final Socket s;
	public final DataInputStream in;
	public final DataOutputStream out;
	public final boolean ssl;
	public int state = 0;
	public String authMethod = "";
	public EmailAccount authUser = null;
	public Mailbox selectedMailbox = null;
	public boolean isExamine = false;
	public long sns = 0L;
	public int tos = 0;
	
	public IMAPWork(Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
		this.s = s;
		this.in = in;
		this.out = out;
		this.ssl = ssl;
	}
	
	public void writeLine(IMAPWork w, String id, String data) throws IOException {
		System.out.println(w.hashCode() + ": " + id + " " + data);
		w.out.write(((id.length() > 0 ? (id + " ") : "") + data + AvunaHTTPD.crlf).getBytes());
	}
}