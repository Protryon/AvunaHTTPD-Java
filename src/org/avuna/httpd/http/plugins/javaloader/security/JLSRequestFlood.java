package org.avuna.httpd.http.plugins.javaloader.security;

import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.plugins.javaloader.JavaLoaderSecurity;

public class JLSRequestFlood extends JavaLoaderSecurity {
	
	private int maxRequestPerSecond = 0, returnWeight = 0;
	private boolean enabled = true;
	
	public void init() {
		if (!pcfg.containsKey("maxRequestPerSecond")) pcfg.put("maxRequestPerSecond", "64");
		if (!pcfg.containsKey("returnWeight")) pcfg.put("returnWeight", "100");
		if (!pcfg.containsKey("enabled")) pcfg.put("enabled", "true");
		this.maxRequestPerSecond = Integer.parseInt((String)pcfg.get("maxRequestPerSecond"));
		this.returnWeight = Integer.parseInt((String)pcfg.get("returnWeight"));
		this.enabled = pcfg.get("enabled").equals("true");
	}
	
	public void reload() {
		init();
	}
	
	@Override
	public int check(String ip) {
		if (!enabled) return 0;
		// TODO: make
		return 0;
	}
	
	@Override
	public int check(RequestPacket req) {
		return 0;
	}
}
