package org.avuna.httpd.http.plugins.javaloader.security;

import java.util.LinkedHashMap;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.plugins.javaloader.JavaLoaderSecurity;

public class JLSUserAgent extends JavaLoaderSecurity {
	
	private int returnWeight = 0;
	private boolean enabled = true;
	private String[] ua = null;
	
	public void init() {
		if (!pcfg.containsNode("returnWeight")) pcfg.insertNode("returnWeight", "100");
		if (!pcfg.containsNode("enabled")) pcfg.insertNode("enabled", "true");
		if (!pcfg.containsNode("userAgents")) pcfg.insertNode("userAgents", "wordpress,sql,php,scan");
		this.returnWeight = Integer.parseInt(pcfg.getNode("returnWeight").getValue());
		this.enabled = pcfg.getNode("enabled").getValue().equals("true");
		this.ua = pcfg.getNode("userAgents").getValue().split(",");
	}
	
	public void reload(LinkedHashMap<String, Object> cfg) {
		init();
	}
	
	@Override
	public int check(RequestPacket req) {
		if (!enabled) return 0;
		if (!req.headers.hasHeader("User-Agent")) {
			return returnWeight;
		}
		String ua = req.headers.getHeader("User-Agent").toLowerCase().trim();
		for (String mua : this.ua) {
			if (ua.contains(mua)) {
				return returnWeight;
			}
		}
		return 0;
	}
	
	@Override
	public int check(String arg0) {
		return 0;
	}
	
}
