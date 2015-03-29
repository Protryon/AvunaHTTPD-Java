package org.avuna.httpd.mail.imap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import javax.net.ssl.SSLServerSocket;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.plugins.PatchRegistry;
import org.avuna.httpd.plugins.base.PatchSecurity;
import org.avuna.httpd.plugins.javaloader.JavaLoaderSecurity;
import org.avuna.httpd.plugins.javaloader.PatchJavaLoader;
import org.avuna.httpd.util.Logger;

/**
 * Handles a single connection.
 */
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
				if (PatchRegistry.getPatchForClass(PatchSecurity.class).pcfg.get("enabled").equals("true")) {
					int minDrop = Integer.parseInt((String)PatchRegistry.getPatchForClass(PatchSecurity.class).pcfg.get("minDrop"));
					int chance = 0;
					for (JavaLoaderSecurity sec : PatchJavaLoader.security) {
						chance += sec.check(s.getInetAddress().getHostAddress());
					}
					if (chance >= minDrop) {
						s.close();
						AvunaHTTPD.bannedIPs.add(s.getInetAddress().getHostAddress());
						continue;
					}
				}
				DataOutputStream out = new DataOutputStream(s.getOutputStream());
				out.flush();
				DataInputStream in = new DataInputStream(s.getInputStream());
				out.write(("* OK " + ((String)host.getConfig().get("domain")).split(",")[0] + " IMAP JavaMailServer ready." + AvunaHTTPD.crlf).getBytes());
				out.flush();
				host.addWorkIMAP(s, in, out, server instanceof SSLServerSocket);
			}catch (Exception e) {
				Logger.logError(e);
			}
		}
	}
}
