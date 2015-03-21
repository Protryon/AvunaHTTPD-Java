package org.avuna.httpd.plugins.javaloader.security;

import java.util.LinkedHashMap;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.plugins.javaloader.JavaLoaderSecurity;

public class JLSUA extends JavaLoaderSecurity {
	
	private int returnWeight = 0;
	private boolean enabled = true;
	private String[] ua = null;
	
	public void init() {
		if (!pcfg.containsKey("returnWeight")) pcfg.put("returnWeight", "100");
		if (!pcfg.containsKey("enabled")) pcfg.put("enabled", "true");
		if (!pcfg.containsKey("userAgents")) pcfg.put("userAgents", "wordpress,sql,php,scan");
		this.returnWeight = Integer.parseInt((String)pcfg.get("returnWeight"));
		this.enabled = pcfg.get("enabled").equals("true");
		this.ua = ((String)pcfg.get("userAgents")).split(",");
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
