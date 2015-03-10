package com.javaprophet.javawebserver.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import javax.net.ssl.SSLServerSocket;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.hosts.Host;
import com.javaprophet.javawebserver.plugins.PatchRegistry;
import com.javaprophet.javawebserver.plugins.base.PatchSecurity;
import com.javaprophet.javawebserver.plugins.javaloader.JavaLoaderSecurity;
import com.javaprophet.javawebserver.plugins.javaloader.PatchJavaLoader;
import com.javaprophet.javawebserver.util.Logger;

public class ThreadAccept extends Thread {
	private final ServerSocket server;
	private final int cl;
	private final Host host;
	private static int nid = 1;
	
	public ThreadAccept(Host host, ServerSocket server, int cl) {
		super("JWS Accept Thread #" + nid++);
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
					JavaWebServer.bannedIPs.clear();
				}
				if (JavaWebServer.bannedIPs.contains(s.getInetAddress().getHostAddress())) {
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
						JavaWebServer.bannedIPs.add(s.getInetAddress().getHostAddress());
						ThreadWorker.clearIPs(s.getInetAddress().getHostAddress());
						continue;
					}
				}
				DataOutputStream out = new DataOutputStream(s.getOutputStream());
				out.flush();
				DataInputStream in = new DataInputStream(s.getInputStream());
				ThreadWorker.addWork(host, s, in, out, server instanceof SSLServerSocket);
			}catch (Exception e) {
				Logger.logError(e);
			}
		}
	}
}
