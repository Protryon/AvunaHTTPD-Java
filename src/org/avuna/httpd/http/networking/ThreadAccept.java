/*
 * Avuna HTTPD - General Server Applications
 * Copyright (C) 2015 Maxwell Bruce
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

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
import org.avuna.httpd.http.plugins.base.PluginSecurity;
import org.avuna.httpd.http.plugins.javaloader.JavaLoaderSecurity;
import org.avuna.httpd.http.plugins.javaloader.PluginJavaLoader;
import org.avuna.httpd.util.Logger;

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
				s.setTcpNoDelay(true);
				if (cl >= 0 && host.sizeQueue() >= cl) {
					s.close();
					continue;
				}
				if (AvunaHTTPD.lastbipc <= System.currentTimeMillis()) {
					AvunaHTTPD.lastbipc = System.currentTimeMillis() + 3600000L;
					AvunaHTTPD.bannedIPs.clear();
				}
				if (!host.isUnix() && AvunaHTTPD.bannedIPs.contains(s.getInetAddress().getHostAddress())) {
					s.close();
					continue; // TODO: move out all security code
				}
				s.setSoTimeout(1000);
				PluginSecurity ps = (PluginSecurity)host.registry.getPatchForClass(PluginSecurity.class);
				if (ps != null && ps.pcfg.getNode("enabled").getValue().equals("true")) {
					int minDrop = Integer.parseInt((String)host.registry.getPatchForClass(PluginSecurity.class).pcfg.getNode("minDrop").getValue());
					int chance = 0;
					for (JavaLoaderSecurity sec : PluginJavaLoader.security) {
						chance += sec.check(s.getInetAddress().getHostAddress());
					}
					if (chance >= minDrop) {
						s.close();
						AvunaHTTPD.bannedIPs.add(s.getInetAddress().getHostAddress());
						host.clearIPs(s.getInetAddress().getHostAddress());
						continue;
					}
				}
				DataOutputStream out = new DataOutputStream(s.getOutputStream());
				out.flush();
				DataInputStream in = new DataInputStream(s.getInputStream());
				EventPreConnect epc = new EventPreConnect(s, out, in);
				host.eventBus.callEvent(epc);
				if (epc.isCanceled()) {
					s.close();
					continue;
				}
				host.addWork(host, s, in, out, server instanceof SSLServerSocket);
			}catch (SocketException e) {
				if (!server.isClosed()) Logger.logError(e);
			}catch (Exception e) {
				Logger.logError(e);
			}
		}
	}
}
