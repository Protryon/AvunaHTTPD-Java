package org.avuna.httpd.mail.imap.command;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.net.ssl.SSLSocket;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.imap.IMAPCommand;
import org.avuna.httpd.mail.imap.IMAPWork;

public class IMAPCommandStarttls extends IMAPCommand {
	
	public IMAPCommandStarttls(String comm, int minState, int maxState, HostMail host) {
		super(comm, minState, maxState, host);
	}
	
	@Override
	public void run(IMAPWork focus, String letters, String[] args) throws IOException {
		focus.writeLine(focus, letters, "OK Begin TLS negotiation now");
		focus.s = host.sslContext.getSocketFactory().createSocket(focus.s, focus.s.getInetAddress().getHostAddress(), focus.s.getPort(), true);
		((SSLSocket)focus.s).setUseClientMode(false);
		((SSLSocket)focus.s).startHandshake();
		focus.out = new DataOutputStream(focus.s.getOutputStream());
		focus.out.flush();
		focus.in = new DataInputStream(focus.s.getInputStream());
		focus.sslprep = new ByteArrayOutputStream();
		focus.ssl = true;
	}
	
}
