package org.avuna.httpd.http.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import javax.net.ssl.SSLServerSocket;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.plugins.PatchRegistry;
import org.avuna.httpd.plugins.base.PatchSecurity;
import org.avuna.httpd.plugins.javaloader.JavaLoaderSecurity;
import org.avuna.httpd.plugins.javaloader.PatchJavaLoader;
import org.avuna.httpd.util.Logger;

public class ThreadAccept extends Thread {
	private final ServerSocket server;
	private final int cl;
	private final HostHTTP host;
	private static int nid = 1;
	
	public ThreadAccept(HostHTTP host, ServerSocket server, int cl) {
		super("JWS Accept Thread #" + nid++);
		setDaemon(true);
		this.server = server;
		this.cl = cl;
		this.host = host;
	}
	
	private static long lastbipc = 0L;
	
	public void run() {
		while (!server.isClosed()) {
			try {
				Socket s = server.accept();
				if (cl >= 0 && ThreadWorker.getQueueSize() >= cl) {
					s.close();
					continue;
				}
				if (lastbipc <= System.currentTimeMillis()) {
					lastbipc = System.currentTimeMillis() + 3600000L;
					AvunaHTTPD.bannedIPs.clear();
				}
				if (AvunaHTTPD.bannedIPs.contains(s.getInetAddress().getHostAddress())) {
					s.close();
					continue;
				}
				s.setSoTimeout(1000);
				if (PatchRegistry.getPatchForClass(PatchSecurity.class).pcfg.get("enabled").equals("true")) {
					int minDrop = Integer.parseInt((String)PatchRegistry.getPatchForClass(PatchSecurity.class).pcfg.get("minDrop"));
					int chance = 0;
					for (JavaLoaderSecurity sec : PatchJavaLoader.security) {
						chance += sec.check(s.getInetAddress().getHostAddress());
					}
					if (chance >= minDrop) {
						s.close();
						AvunaHTTPD.bannedIPs.add(s.getInetAddress().getHostAddress());
						ThreadConnection.clearIPs(s.getInetAddress().getHostAddress());
						continue;
					}
				}
				DataOutputStream out = new DataOutputStream(s.getOutputStream());
				out.flush();
				DataInputStream in = new DataInputStream(s.getInputStream());
				ThreadConnection.addWork(host, s, in, out, server instanceof SSLServerSocket);
			}catch (Exception e) {
				Logger.logError(e);
			}
		}
	}
}
