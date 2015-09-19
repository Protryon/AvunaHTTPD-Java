/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import javax.net.ssl.SSLServerSocket;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.event.base.EventPreConnect;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.util.unio.UNIOSocket;

public class ThreadAccept extends Thread {
	private final ServerSocket server;
	private final int cl;
	private final HostHTTP host;
	private static int nid = 1;
	
	public ThreadAccept(HostHTTP host, ServerSocket server, int cl) {
		super("Avuna HTTP-Accept Thread #" + nid++);
		this.server = server;
		this.cl = cl;
		this.host = host;
	}
	
	public void run() {
		while (!server.isClosed()) {
			try {
				Socket s = server.accept();
				if (!host.loaded) {
					s.close();
				}
				s.setTcpNoDelay(true);
				s.setSoTimeout(1000);
				if (cl >= 0 && host.works.size() >= cl) {
					s.close();
					continue;
				}
				if (AvunaHTTPD.lastbipc <= System.currentTimeMillis()) {
					AvunaHTTPD.lastbipc = System.currentTimeMillis() + 3600000L;
					AvunaHTTPD.bannedIPs.clear();
				}
				if (!host.isUnix() && AvunaHTTPD.bannedIPs.contains(s.getInetAddress().getHostAddress())) {
					s.close();
					continue;
				}
				if (s instanceof UNIOSocket) {
					((UNIOSocket) s).setTimeout(10000L);
				}
				DataOutputStream out = new DataOutputStream(s.getOutputStream());
				out.flush();
				DataInputStream in = new DataInputStream(s.getInputStream());
				EventPreConnect epc = new EventPreConnect(host, s, out, in);
				host.eventBus.callEvent(epc);
				if (epc.isCanceled()) {
					s.close();
					continue;
				}
				host.addWork(s, in, out, server instanceof SSLServerSocket || (s instanceof UNIOSocket && ((UNIOSocket) s).isSecure()));
			}catch (SocketException e) {
				if (!server.isClosed()) host.logger.logError(e);
			}catch (Exception e) {
				host.logger.logError(e);
			}
		}
	}
}
