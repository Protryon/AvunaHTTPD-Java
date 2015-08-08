/*
 * Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.mail.mailbox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Comparator;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.util.Stream;

public class EmailRouter {
	public static void route(HostMail host, Email email) {
		for (String to : email.to) {
			if (to.contains("<") && to.contains(">")) {
				to = to.substring(to.indexOf("<") + 1, to.indexOf(">"));
			}
			String dom = to.substring(to.indexOf("@") + 1);
			String[] doms = host.getConfig().getNode("domain").getValue().split(",");
			boolean local = false;
			for (String dommie : doms) {
				if (dommie.trim().equals(dom)) {
					local = true;
					break;
				}
			}
			if (local) {
				for (EmailAccount acct : host.accounts) {
					if (acct.email.equals(to)) {
						acct.deliver(email);
					}
				}
			}else {
				String[] mx = new String[0];
				try {
					mx = lookupMailHosts(dom);
				}catch (NamingException e) {
					e.printStackTrace();
				}
				int lrt = 0;
				boolean delivered = false;
				for (int i = 0; i < mx.length; i++) {
					String mxr = mx[i];
					host.logger.log("Trying " + mxr);
					Socket rs = null;
					try {
						rs = new Socket(InetAddress.getByName(mxr), 25);
						DataOutputStream out = new DataOutputStream(rs.getOutputStream());
						out.flush();
						DataInputStream in = new DataInputStream(rs.getInputStream());
						String header = Stream.readLine(in);
						host.logger.log(header);
						out.write(("EHLO " + doms[0] + AvunaHTTPD.crlf).getBytes());
						host.logger.log("EHLO " + doms[0]);
						out.flush();
						String line;
						while ((line = Stream.readLine(in)).length() >= 4 && line.charAt(3) == '-')
							host.logger.log(line);
						out.write(("MAIL FROM: " + email.from + AvunaHTTPD.crlf).getBytes());
						host.logger.log("MAIL FROM: " + email.from);
						out.flush();
						String mresp = Stream.readLine(in);
						host.logger.log(mresp);
						if (!mresp.startsWith("2")) {
							if (mresp.startsWith("4")) {
								if (lrt < 3) {
									i--;
									lrt++;
								}else {
									lrt = 0;
								}
								continue;
							}else {
								continue;
							}
						}
						if (!to.startsWith("<")) to = "<" + to + ">";
						out.write(("RCPT TO: " + to + AvunaHTTPD.crlf).getBytes());
						host.logger.log("RCPT TO: " + to);
						out.flush();
						String rresp = Stream.readLine(in);
						host.logger.log(rresp);
						if (!rresp.startsWith("2")) {
							if (rresp.startsWith("4")) {
								if (lrt < 3) {
									i--;
									lrt++;
								}else {
									lrt = 0;
								}
								continue;
							}else {
								continue;
							}
						}
						out.write(("DATA" + AvunaHTTPD.crlf).getBytes());
						host.logger.log("DATA");
						out.flush();
						String dresp = Stream.readLine(in);
						host.logger.log(dresp);
						if (!dresp.startsWith("354")) {
							if (dresp.startsWith("4")) {
								if (lrt < 3) {
									i--;
									lrt++;
								}else {
									lrt = 0;
								}
								continue;
							}else {
								continue;
							}
						}
						out.write(email.data.getBytes());
						host.logger.log(email.data);
						out.write((AvunaHTTPD.crlf + "." + AvunaHTTPD.crlf).getBytes());
						out.flush();
						String fresp = Stream.readLine(in);
						host.logger.log(fresp);
						if (!fresp.startsWith("2")) {
							if (fresp.startsWith("4")) {
								if (lrt < 3) {
									i--;
									lrt++;
								}else {
									lrt = 0;
								}
								continue;
							}else {
								continue;
							}
						}
						delivered = true;
						break;
					}catch (Exception e) {
						host.logger.logError(e);
					}finally {
						if (rs != null) try {
							rs.close();
						}catch (IOException e) {
							host.logger.logError(e);
						}
					}
				}
				if (!delivered) {
					host.logger.log("Mail was not accepted from any server listed in MX!"); // TODO: try again then send a no reciept email back
				}
			}
		}
	}
	
	// CREDIT: http://www.eyeasme.com/Shayne/MAILHOSTS/mailHostsLookup.html Copyright � 2011 Shayne Steele (shayne.steele@eyeasme.com)
	private static String[] lookupMailHosts(String domainName) throws NamingException {
		InitialDirContext iDirC = new InitialDirContext();
		Attributes attributes = iDirC.getAttributes("dns:/" + domainName, new String[] { "MX" });
		Attribute attributeMX = attributes.get("MX");
		if (attributeMX == null) {
			return (new String[] { domainName });
		}
		String[][] pvhn = new String[attributeMX.size()][2];
		for (int i = 0; i < attributeMX.size(); i++) {
			pvhn[i] = ("" + attributeMX.get(i)).split("\\s+");
		}
		Arrays.sort(pvhn, new Comparator<String[]>() {
			public int compare(String[] o1, String[] o2) {
				return (Integer.parseInt(o1[0]) - Integer.parseInt(o2[0]));
			}
		});
		String[] sortedHostNames = new String[pvhn.length];
		for (int i = 0; i < pvhn.length; i++) {
			sortedHostNames[i] = pvhn[i][1].endsWith(".") ? pvhn[i][1].substring(0, pvhn[i][1].length() - 1) : pvhn[i][1];
		}
		return sortedHostNames;
	}
}
