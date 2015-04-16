package org.avuna.httpd.mail.smtp;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.mail.mailbox.EmailAccount;
import org.avuna.httpd.util.Logger;

public class SMTPWork {
	public Socket s;
	public DataInputStream in;
	public DataOutputStream out;
	public boolean ssl;
	public int state = 0;
	public boolean isExtended = false;
	public EmailAccount authUser = null;
	public String mailFrom = "";
	public long sns = 0L;
	public int tos = 0;
	public ArrayList<String> rcptTo = new ArrayList<String>();
	public ArrayList<String> data = new ArrayList<String>();
	public ByteArrayOutputStream sslprep = null;
	
	public SMTPWork(Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
		this.s = s;
		this.in = in;
		this.out = out;
		this.ssl = ssl;
		if (ssl) {
			sslprep = new ByteArrayOutputStream();
		}
	}
	
	public void writeMLine(int response, String data) throws IOException {
		Logger.log(hashCode() + ": " + response + "-" + data);
		out.write((response + "-" + data + AvunaHTTPD.crlf).getBytes());
	}
	
	public void writeLine(int response, String data) throws IOException {
		Logger.log(hashCode() + ": " + response + " " + data);
		out.write((response + " " + data + AvunaHTTPD.crlf).getBytes());
	}
}
