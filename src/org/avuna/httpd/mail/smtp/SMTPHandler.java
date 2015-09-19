/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.mail.smtp;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import javax.net.ssl.SSLSocket;
import javax.xml.bind.DatatypeConverter;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.mailbox.Email;
import org.avuna.httpd.mail.mailbox.EmailAccount;
import org.avuna.httpd.mail.mailbox.EmailRouter;
import org.avuna.httpd.util.CLib;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.unio.Certificate;
import org.avuna.httpd.util.unio.UNIOServerSocket;
import org.avuna.httpd.util.unio.UNIOSocket;

public class SMTPHandler {
	
	public final ArrayList<SMTPCommand> commands = new ArrayList<SMTPCommand>();
	
	public SMTPHandler(final HostMail host) {
		commands.add(new SMTPCommand("helo", 0, 100) {
			public void run(SMTPWork focus, String line) throws IOException {
				focus.writeLine(250, "OK");
				focus.state = 1;
				focus.isExtended = false;
			}
		});
		
		commands.add(new SMTPCommand("starttls", 0, 100) {
			public void run(SMTPWork focus, String line) throws IOException {
				focus.writeLine(220, "Go ahead");
				if (host.unio() && CLib.hasGNUTLS() == 1) {
					if (host.smtps == null) {
						focus.writeLine(520, "TLS not enabled!");
						return;
					}
					Certificate cert = ((UNIOServerSocket) host.smtps).getCertificate();
					if (cert == null) {
						focus.writeLine(520, "TLS not enabled!");
						return;
					}
					((UNIOSocket) focus.s).starttls(cert, ((UNIOServerSocket) host.smtps).getSNICallback());
					focus.ssl = true;
				}else {
					if (host.sslContext == null) {
						focus.writeLine(520, "TLS not enabled!");
						return;
					}
					focus.s = host.sslContext.getSocketFactory().createSocket(focus.s, focus.s.getInetAddress().getHostAddress(), focus.s.getPort(), true);
					((SSLSocket) focus.s).setUseClientMode(false);
					((SSLSocket) focus.s).setNeedClientAuth(false);
					((SSLSocket) focus.s).startHandshake();
					focus.out = new DataOutputStream(focus.s.getOutputStream());
					focus.out.flush();
					focus.in = new DataInputStream(focus.s.getInputStream());
					focus.sslprep = new ByteArrayOutputStream();
					focus.ssl = true;
					focus.state = 1;
				}
			}
		});
		
		commands.add(new SMTPCommand("ehlo", 0, 100) {
			public void run(SMTPWork focus, String line) throws IOException {
				focus.writeMLine(250, host.getConfig().getNode("domain").getValue().split(",")[0]);
				focus.writeMLine(250, "AUTH PLAIN LOGIN");
				if (host.smtps != null) focus.writeMLine(250, "STARTTLS");
				focus.writeLine(250, "AUTH=PLAIN LOGIN");
				focus.state = 1;
				focus.isExtended = true;
			}
		});
		
		commands.add(new SMTPCommand("auth", 1, 100) {
			public void run(SMTPWork focus, String lp) throws IOException {
				String line = lp;
				if (line.toUpperCase().startsWith("PLAIN")) {
					line = line.substring(6).trim();
					if (line.length() > 0) {
						String up = new String(DatatypeConverter.parseBase64Binary(line));
						if (up.length() > 0 && up.charAt(0) == 0) up = up.substring(1);
						String username = up.substring(0, up.indexOf(new String(new byte[] { 0 })));
						String password = up.substring(username.length() + 1);
						EmailAccount us = null;
						for (EmailAccount e : host.accounts) {
							if (e.email.equals(username) && e.password.equals(password)) {
								us = e;
								break;
							}
						}
						if (us != null) {
							focus.writeLine(235, "OK");
							focus.authUser = us;
							focus.state = 2;
						}else {
							focus.writeLine(535, "authentication failed");
						}
					}else {
						focus.writeLine(501, "Syntax error in parameters or arguments");
					}
				}else if (line.toUpperCase().startsWith("LOGIN")) {
					focus.writeLine(334, "VXNlcm5hbWU6");
					focus.state = 102;
				}else {
					focus.writeLine(501, "Syntax error in parameters or arguments");
				}
			}
		});
		
		commands.add(new SMTPCommand("", 102, 102) {
			public void run(SMTPWork focus, String lp) throws IOException {
				focus.lu = new String(DatatypeConverter.parseBase64Binary(lp.trim()));
				focus.writeLine(334, "UGFzc3dvcmQ6");
				focus.state = 103;
			}
		});
		
		commands.add(new SMTPCommand("", 103, 103) {
			public void run(SMTPWork focus, String lp) throws IOException {
				String username = focus.lu;
				String password = new String(DatatypeConverter.parseBase64Binary(lp.trim()));
				EmailAccount us = null;
				for (EmailAccount e : host.accounts) {
					if (e.email.equals(username) && e.password.equals(password)) {
						us = e;
						break;
					}
				}
				if (us != null) {
					focus.writeLine(235, "OK");
					focus.authUser = us;
					focus.state = 2;
				}else {
					focus.writeLine(535, "authentication failed");
					focus.state = 1;
				}
				
			}
		});
		
		commands.add(new SMTPCommand("mail", 1, 3) {
			public void run(SMTPWork focus, String line) throws IOException {
				if (line.toLowerCase().startsWith("from:")) {
					focus.mailFrom = line.substring(5).trim();
					String[] doms = null;
					ConfigNode domsn = host.getConfig().getNode("domain");
					synchronized (domsn) {
						doms = domsn.getValue().split(",");
					}
					boolean gd = false;
					for (String dom : doms) {
						if (focus.mailFrom.endsWith(dom)) {
							gd = true;
							break;
						}
					}
					if (gd && focus.authUser == null) {
						focus.writeLine(535, "NO Not Authorized");
						return;
					}
					focus.rcptTo.clear();
					focus.data.clear();
					focus.state = 3;
					focus.writeLine(250, "OK");
				}else {
					focus.writeLine(500, "Lacking FROM");
				}
			}
		});
		
		commands.add(new SMTPCommand("rcpt", 3, 4) {
			public void run(SMTPWork focus, String line) throws IOException {
				if (line.toLowerCase().startsWith("to:")) {
					String to = line.substring(3).trim();
					boolean local = false;
					String[] doms = null;
					ConfigNode domsn = host.getConfig().getNode("domain");
					synchronized (domsn) {
						doms = domsn.getValue().split(",");
					}
					for (String domain : doms) {
						if (to.endsWith(domain) || (to.contains("<") && to.endsWith(">") && to.substring(to.indexOf("<") + 1, to.length() - 1).endsWith(domain))) {
							local = true;
							break;
						}
					}
					if (focus.authUser == null && !local) {
						focus.writeLine(535, "Denied");
						return;
					}
					focus.rcptTo.add(to);
					focus.state = 4;
					focus.writeLine(250, "OK");
				}else {
					focus.writeLine(500, "Lacking TO");
				}
			}
		});
		
		commands.add(new SMTPCommand("data", 4, 4) {
			public void run(SMTPWork focus, String line) throws IOException {
				focus.state = 101;
				focus.writeLine(354, "Start mail input; end with <CRLF>.<CRLF>");
			}
		});
		
		commands.add(new SMTPCommand("", 101, 101) {
			public void run(SMTPWork focus, String line) throws IOException {
				if (!line.equals(".")) {
					focus.data.add(line);
				}else {
					focus.state = 1;
					String data = "";
					for (String l : focus.data) {
						data += l + AvunaHTTPD.crlf;
					}
					Email email = new Email(host, data, -1, focus.mailFrom);
					email.to.addAll(focus.rcptTo);
					EmailRouter.route(host, email);
					focus.writeLine(250, "OK");
				}
			}
		});
		
		commands.add(new SMTPCommand("quit", 1, 100) {
			public void run(SMTPWork focus, String line) throws IOException {
				String dv = null;
				ConfigNode doms = host.getConfig().getNode("domain");
				synchronized (doms) {
					dv = doms.getValue().split(",")[0];
				}
				focus.writeLine(221, dv + " terminating connection.");
				focus.s.close();
			}
		});
	}
}
