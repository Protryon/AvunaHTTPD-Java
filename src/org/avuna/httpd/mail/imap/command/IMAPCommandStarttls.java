/*
 * Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

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
		focus.writeLine(letters, "OK Begin TLS negotiation now");
		focus.s = host.sslContext.getSocketFactory().createSocket(focus.s, focus.s.getInetAddress().getHostAddress(), focus.s.getPort(), true);
		((SSLSocket) focus.s).setUseClientMode(false);
		((SSLSocket) focus.s).setNeedClientAuth(false);
		((SSLSocket) focus.s).startHandshake();
		focus.out = new DataOutputStream(focus.s.getOutputStream());
		focus.out.flush();
		focus.in = new DataInputStream(focus.s.getInputStream());
		focus.sslprep = new ByteArrayOutputStream();
		focus.ssl = true;
	}
	
}
