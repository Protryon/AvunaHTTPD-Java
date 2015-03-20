package com.javaprophet.javawebserver.plugins.javaloader.security;

import com.javaprophet.javawebserver.networking.ThreadConnection;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.plugins.javaloader.JavaLoaderSecurity;

public class JLSBFlood extends JavaLoaderSecurity {
	
	private int maxConcurrentConns = 0, returnWeight = 0;
	private boolean enabled = true;
	
	public void init() {
		if (!pcfg.containsKey("maxConcurrentConns")) pcfg.put("maxConcurrentConns", "50");
		if (!pcfg.containsKey("returnWeight")) pcfg.put("returnWeight", "100");
		if (!pcfg.containsKey("enabled")) pcfg.put("enabled", "true");
		this.maxConcurrentConns = Integer.parseInt((String)pcfg.get("maxConcurrentConns"));
		this.returnWeight = Integer.parseInt((String)pcfg.get("returnWeight"));
		this.enabled = pcfg.get("enabled").equals("true");
	}
	
	public void reload() {
		init();
	}
	
	@Override
	public int check(String ip) {
		if (!enabled) return 0;
		int ips = ThreadConnection.getConnectionsForIP(ip);
		if (ips > maxConcurrentConns) {
			return returnWeight;
		}
		return 0;
	}
	
	@Override
	public int check(RequestPacket req) {
		return 0;
	}
}
