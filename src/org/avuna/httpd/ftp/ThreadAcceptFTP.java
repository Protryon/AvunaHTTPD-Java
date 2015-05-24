package org.avuna.httpd.ftp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostFTP;
import org.avuna.httpd.util.Logger;

/**
 * Handles a single connection.
 */
public class ThreadAcceptFTP extends Thread {
	
	private final ServerSocket server;
	private final int cl;
	private final HostFTP host;
	private static int nid = 1;
	
	public ThreadAcceptFTP(HostFTP host, ServerSocket server, int cl) {
		super("Avuna FTP-Accept Thread #" + nid++);
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
				if (server instanceof SSLServerSocket) {
					((SSLSocket)s).startHandshake();
				}
				out.write(("220 " + host.getConfig().getNode("domain").getValue().split(",")[0] + " ESMTP Avuna-HTTPD" + AvunaHTTPD.crlf).getBytes());
				out.flush();
				s.setSoTimeout(1000);
				host.addWork(s, in, out);
			}catch (Exception e) {
				Logger.logError(e);
			}
		}
	}
}
