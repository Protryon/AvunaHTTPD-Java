package org.avuna.httpd.http.plugins.javaloader.security;

import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.plugins.javaloader.JavaLoaderSecurity;
import org.avuna.httpd.util.Logger;

public class JLSConnectionFlood extends JavaLoaderSecurity {
	
	private int maxConcurrentConns = 0, returnWeight = 0;
	private boolean enabled = true;
	
	public void init() {
		if (!pcfg.containsNode("maxConcurrentConns")) pcfg.insertNode("maxConcurrentConns", "50");
		if (!pcfg.containsNode("returnWeight")) pcfg.insertNode("returnWeight", "100");
		if (!pcfg.containsNode("enabled")) pcfg.insertNode("enabled", "true");
		this.maxConcurrentConns = Integer.parseInt(pcfg.getNode("maxConcurrentConns").getValue());
		this.returnWeight = Integer.parseInt(pcfg.getNode("returnWeight").getValue());
		this.enabled = pcfg.getNode("enabled").getValue().equals("true");
	}
	
	public void reload() {
		init();
	}
	
	@Override
	public int check(String ip) {
		if (!enabled) return 0;
		int ips = HostHTTP.getConnectionsForIP(ip);
		if (ips > maxConcurrentConns) {
			Logger.log("Connection abuse from " + ip + ", may be banned depending on configuration.");
			return returnWeight;
		}
		return 0;
	}
	
	@Override
	public int check(RequestPacket req) {
		return 0;
	}
}
