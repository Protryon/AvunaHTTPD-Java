package com.javaprophet.javawebserver.plugins.javaloader.security;

import java.util.LinkedHashMap;
import com.javaprophet.javawebserver.hosts.VHost;
import com.javaprophet.javawebserver.http.Method;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.plugins.javaloader.JavaLoaderSecurity;

public class JLSBProxy extends JavaLoaderSecurity {
	
	private int returnWeight = 0;
	private boolean enabled = true;
	
	public void init(VHost host, LinkedHashMap<String, Object> cfg) {
		if (!cfg.containsKey("returnWeight")) cfg.put("returnWeight", "100");
		if (!cfg.containsKey("enabled")) cfg.put("enabled", "true");
		this.returnWeight = Integer.parseInt((String)cfg.get("returnWeight"));
		this.enabled = cfg.get("enabled").equals("true");
	}
	
	public void reload(LinkedHashMap<String, Object> cfg) {
		init(null, cfg);
	}
	
	@Override
	public int check(RequestPacket req) {
		if (!enabled) return 0;
		if (req.method == Method.POST) {
			if (req.headers.hasHeader("Content-Type")) {
				if (req.headers.getHeader("Content-Type").startsWith("application/x-www-form-urlencoded")) {
					String body = new String(req.body.data);
					if (!body.contains("=")) {
						return returnWeight;
					}
				}
			}else {
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
