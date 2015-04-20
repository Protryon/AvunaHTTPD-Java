package org.avuna.httpd.http.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import javax.net.ssl.SSLServerSocket;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.http.plugins.base.PatchSecurity;
import org.avuna.httpd.http.plugins.javaloader.JavaLoaderSecurity;
import org.avuna.httpd.http.plugins.javaloader.PatchJavaLoader;
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
				if (cl >= 0 && host.sizeQueue() >= cl) {
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
				PatchSecurity ps = (PatchSecurity)host.registry.getPatchForClass(PatchSecurity.class);
				if (ps != null && ps.pcfg.getNode("enabled").getValue().equals("true")) {
					int minDrop = Integer.parseInt((String)host.registry.getPatchForClass(PatchSecurity.class).pcfg.getNode("minDrop").getValue());
					int chance = 0;
					for (JavaLoaderSecurity sec : PatchJavaLoader.security) {
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
				host.addWork(host, s, in, out, server instanceof SSLServerSocket);
			}catch (Exception e) {
				Logger.logError(e);
			}
		}
	}
}
