package org.avuna.httpd.mail.smtp;

import java.io.IOException;
import java.util.ArrayList;
import javax.xml.bind.DatatypeConverter;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.mailbox.Email;
import org.avuna.httpd.mail.mailbox.EmailAccount;
import org.avuna.httpd.mail.mailbox.EmailRouter;

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
		
		commands.add(new SMTPCommand("ehlo", 0, 100) {
			public void run(SMTPWork focus, String line) throws IOException {
				focus.writeMLine(250, ((String)host.getConfig().get("domain")).split(",")[0]);
				focus.writeMLine(250, "AUTH PLAIN LOGIN");
				focus.writeLine(250, "AUTH=PLAIN LOGIN");
				focus.state = 1;
				focus.isExtended = true;
			}
		});
		
		commands.add(new SMTPCommand("auth", 1, 100) {
			public void run(SMTPWork focus, String line) throws IOException {
				if (line.toUpperCase().startsWith("PLAIN")) {
					line = line.substring(6).trim();
					if (line.length() > 0) {
						String up = new String(DatatypeConverter.parseBase64Binary(line)).substring(1);
						String username = up.substring(0, up.indexOf(new String(new byte[]{0})));
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
					String u64 = focus.in.readLine().trim();
					String username = new String(DatatypeConverter.parseBase64Binary(u64));
					focus.writeLine(334, "UGFzc3dvcmQ6");
					String p64 = focus.in.readLine().trim();
					String password = new String(DatatypeConverter.parseBase64Binary(p64));
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
			}
		});
		
		commands.add(new SMTPCommand("mail", 1, 3) {
			public void run(SMTPWork focus, String line) throws IOException {
				if (line.toLowerCase().startsWith("from:")) {
					focus.mailFrom = line.substring(5).trim();
					String[] doms = ((String)host.getConfig().get("domain")).split(",");
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
					for (String domain : ((String)host.getConfig().get("domain")).split(",")) {
						if (to.endsWith(domain) || (to.startsWith("<") && to.endsWith(">") && to.substring(1, to.length() - 1).endsWith(domain))) {
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
					Email email = new Email(data, -1, focus.mailFrom);
					email.to.addAll(focus.rcptTo);
					EmailRouter.route(host, email);
					focus.writeLine(250, "OK");
				}
			}
		});
		
		commands.add(new SMTPCommand("quit", 1, 100) {
			public void run(SMTPWork focus, String line) throws IOException {
				focus.writeLine(251, ((String)host.getConfig().get("domain")).split(",")[0] + " terminating connection.");
				focus.s.close();
			}
		});
	}
}
