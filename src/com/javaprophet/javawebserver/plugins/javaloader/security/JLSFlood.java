package com.javaprophet.javawebserver.plugins.javaloader.security;

import java.util.LinkedHashMap;
import com.javaprophet.javawebserver.hosts.VHost;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.plugins.javaloader.JavaLoaderSecurity;

public class JLSFlood extends JavaLoaderSecurity {
	
	private int returnWeight = 0;
	private boolean enabled = true;
	private String regex = "";
	
	public void init(VHost host, LinkedHashMap<String, Object> cfg) {
		if (!cfg.containsKey("returnWeight")) cfg.put("returnWeight", "100");
		if (!cfg.containsKey("enabled")) cfg.put("enabled", "true");
		if (!cfg.containsKey("regex")) cfg.put("regex", "/\\?[0-9a-zA-Z]{2,}");
		this.returnWeight = Integer.parseInt((String)cfg.get("returnWeight"));
		this.enabled = cfg.get("enabled").equals("true");
		this.regex = (String)cfg.get("regex");
	}
	
	public void reload(LinkedHashMap<String, Object> cfg) {
		init(null, cfg);
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
