/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.mail.smtp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.unio.UNIOSocket;

/** Handles a single connection. */
public class ThreadAcceptSMTP extends Thread {
	
	private final ServerSocket server;
	private final int cl;
	private final HostMail host;
	private static int nid = 1;
	
	public ThreadAcceptSMTP(HostMail host, ServerSocket server, int cl) {
		super("Avuna SMTP-Accept Thread #" + nid++);
		this.server = server;
		this.cl = cl;
		this.host = host;
	}
	
	public void run() {
		while (!server.isClosed()) {
			try {
				Socket s = server.accept();
				if (cl >= 0 && host.SMTPworks.size() >= cl) {
					s.close();
					continue;
				}
				if (AvunaHTTPD.lastbipc <= System.currentTimeMillis()) {
					AvunaHTTPD.lastbipc = System.currentTimeMillis() + 3600000L;
					AvunaHTTPD.bannedIPs.clear();
				}
				if (AvunaHTTPD.bannedIPs.contains(s.getInetAddress().getHostAddress())) {
					s.close();
					continue;
				}
				s.setSoTimeout(1000);
				if (s instanceof UNIOSocket) {
					((UNIOSocket) s).setTimeout(30000L);
				}
				// if (PatchRegistry.getPatchForClass(PatchSecurity.class).pcfg.get("enabled").equals("true")) {
				// int minDrop = Integer.parseInt((String)PatchRegistry.getPatchForClass(PatchSecurity.class).pcfg.get("minDrop"));
				// int chance = 0;
				// for (JavaLoaderSecurity sec : PatchJavaLoader.security) {
				// chance += sec.check(s.getInetAddress().getHostAddress());
				// }
				// if (chance >= minDrop) {
				// s.close();
				// AvunaHTTPD.bannedIPs.add(s.getInetAddress().getHostAddress());
				// continue;
				// }
				// }
				DataOutputStream out = new DataOutputStream(s.getOutputStream());
				out.flush();
				DataInputStream in = new DataInputStream(s.getInputStream());
				if (server instanceof SSLServerSocket) {
					((SSLSocket) s).startHandshake();
				}
				String dv = null;
				ConfigNode doms = host.getConfig().getNode("domain");
				synchronized (doms) {
					dv = doms.getValue().split(",")[0];
				}
				out.write(("220 " + dv + " ESMTP Avuna-HTTPD" + AvunaHTTPD.crlf).getBytes());
				out.flush();
				s.setSoTimeout(1000);
				host.addWorkSMTP(s, in, out, server instanceof SSLServerSocket);
			}catch (Exception e) {
				if (!(e instanceof IOException)) host.logger.logError(e);
			}
		}
	}
}
