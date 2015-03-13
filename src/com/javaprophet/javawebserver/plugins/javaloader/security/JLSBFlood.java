package com.javaprophet.javawebserver.plugins.javaloader.security;

import java.util.LinkedHashMap;
import com.javaprophet.javawebserver.hosts.VHost;
import com.javaprophet.javawebserver.networking.ThreadConnection;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.plugins.javaloader.JavaLoaderSecurity;

public class JLSBFlood extends JavaLoaderSecurity {
	
	private int maxConcurrentConns = 0, returnWeight = 0;
	private boolean enabled = true;
	
	public void init(VHost host, LinkedHashMap<String, Object> cfg) {
		if (!cfg.containsKey("maxConcurrentConns")) cfg.put("maxConcurrentConns", "50");
		if (!cfg.containsKey("returnWeight")) cfg.put("returnWeight", "100");
		if (!cfg.containsKey("enabled")) cfg.put("enabled", "true");
		this.maxConcurrentConns = Integer.parseInt((String)cfg.get("maxConcurrentConns"));
		this.returnWeight = Integer.parseInt((String)cfg.get("returnWeight"));
		this.enabled = cfg.get("enabled").equals("true");
	}
	
	public void reload(LinkedHashMap<String, Object> cfg) {
		init(null, cfg);
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
