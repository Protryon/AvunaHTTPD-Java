package org.avuna.httpd.http.plugins.javaloader.security;

import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.plugins.javaloader.JavaLoaderSecurity;
import org.avuna.httpd.util.Logger;

public class JLSBFlood extends JavaLoaderSecurity {
	
	private int maxConcurrentConns = 0, returnWeight = 0;
	private boolean enabled = true;
	private String[] pardoned = new String[0];
	
	public void init() {
		if (!pcfg.containsKey("maxConcurrentConns")) pcfg.put("maxConcurrentConns", "50");
		if (!pcfg.containsKey("returnWeight")) pcfg.put("returnWeight", "100");
		if (!pcfg.containsKey("pardoned")) pcfg.put("pardoned", "127.0.0.1,127.0.0.2");
		if (!pcfg.containsKey("enabled")) pcfg.put("enabled", "true");
		this.maxConcurrentConns = Integer.parseInt((String)pcfg.get("maxConcurrentConns"));
		this.returnWeight = Integer.parseInt((String)pcfg.get("returnWeight"));
		this.enabled = pcfg.get("enabled").equals("true");
		pardoned = ((String)pcfg.get("pardoned")).split(",");
	}
	
	public void reload() {
		init();
	}
	
	@Override
	public int check(String ip) {
		if (!enabled) return 0;
		for (String p : pardoned) {
			if (p.trim().equals(ip)) {
				return 0;
			}
		}
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
