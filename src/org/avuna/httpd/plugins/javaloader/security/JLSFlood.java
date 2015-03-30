package org.avuna.httpd.plugins.javaloader.security;

import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.plugins.javaloader.JavaLoaderSecurity;

public class JLSFlood extends JavaLoaderSecurity {
	
	private int returnWeight = 0;
	private boolean enabled = true;
	private String regex = "";
	
	public void init() {
		if (!pcfg.containsKey("returnWeight")) pcfg.put("returnWeight", "100");
		if (!pcfg.containsKey("enabled")) pcfg.put("enabled", "true");
		if (!pcfg.containsKey("regex")) pcfg.put("regex", "/\\?[0-9a-zA-Z]{2,}");
		this.returnWeight = Integer.parseInt((String)pcfg.get("returnWeight"));
		this.enabled = pcfg.get("enabled").equals("true");
		this.regex = (String)pcfg.get("regex");
	}
	
	public void reload() {
		init();
	}
	
	@Override
	public int check(RequestPacket req) {
		if (!enabled) return 0;
		if (req.target.matches(regex)) {
			return returnWeight;
		}
		return 0;
	}
	
	@Override
	public int check(String arg0) {
		return 0;
	}
	
}