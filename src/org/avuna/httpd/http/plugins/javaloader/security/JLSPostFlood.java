package org.avuna.httpd.http.plugins.javaloader.security;

import org.avuna.httpd.http.Method;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.plugins.javaloader.JavaLoaderSecurity;

public class JLSPostFlood extends JavaLoaderSecurity {
	
	private int returnWeight = 0;
	private boolean enabled = true;
	
	public void init() {
		if (!pcfg.containsNode("returnWeight")) pcfg.insertNode("returnWeight", "100");
		if (!pcfg.containsNode("enabled")) pcfg.insertNode("enabled", "true");
		this.returnWeight = Integer.parseInt(pcfg.getNode("returnWeight").getValue());
		this.enabled = pcfg.getNode("enabled").getValue().equals("true");
	}
	
	public void reload() {
		init();
	}
	
	@Override
	public int check(RequestPacket req) {
		if (!enabled) return 0;
		if (req.method == Method.POST) {
			if (req.headers.hasHeader("Content-Type")) {
				if (req.headers.getHeader("Content-Type").startsWith("application/x-www-form-urlencoded")) {
					String body = new String(req.body.data);
					if (!body.contains("=") && body.length() > 0) {
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
