/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.mail.imap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import javax.net.ssl.SSLServerSocket;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostMail;

/** Handles a single connection. */
public class ThreadAcceptIMAP extends Thread {
	
	private final ServerSocket server;
	private final int cl;
	private final HostMail host;
	private static int nid = 1;
	
	public ThreadAcceptIMAP(HostMail host, ServerSocket server, int cl) {
		super("Avuna IMAP-Accept Thread #" + nid++);
		this.server = server;
		this.cl = cl;
		this.host = host;
	}
	
	public void run() {
		while (!server.isClosed()) {
			try {
				Socket s = server.accept();
				if (cl >= 0 && host.getQueueSizeSMTP() >= cl) {
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
				out.write(("* OK " + host.getConfig().getNode("domain").getValue().split(",")[0] + " IMAP Avuna-HTTPD ready." + AvunaHTTPD.crlf).getBytes());
				out.flush();
				host.addWorkIMAP(s, in, out, server instanceof SSLServerSocket);
			}catch (Exception e) {
				if (!(e instanceof IOException)) host.logger.logError(e);
			}
		}
	}
}
