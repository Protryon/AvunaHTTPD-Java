package org.avuna.httpd.http.plugins.javaloader.security;

import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.plugins.javaloader.JavaLoaderSecurity;

public class JLSRequestFlood extends JavaLoaderSecurity {
	
	private int maxRequestPerSecond = 0, returnWeight = 0;
	private boolean enabled = true;
	
	public void init() {
		if (!pcfg.containsNode("maxRequestPerSecond")) pcfg.insertNode("maxRequestPerSecond", "64");
		if (!pcfg.containsNode("returnWeight")) pcfg.insertNode("returnWeight", "100");
		if (!pcfg.containsNode("enabled")) pcfg.insertNode("enabled", "true");
		this.maxRequestPerSecond = Integer.parseInt(pcfg.getNode("maxRequestPerSecond").getValue());
		this.returnWeight = Integer.parseInt(pcfg.getNode("returnWeight").getValue());
		this.enabled = pcfg.getNode("enabled").getValue().equals("true");
	}
	
	public void reload() {
		init();
	}
	
	@Override
	public int check(String ip) {
		return 0;
	}
	
	@Override
	public int check(RequestPacket req) {
		if (!enabled) return 0;
		double rqps = (double)req.work.rqs / (((double)req.work.rqst - (double)System.currentTimeMillis()) / 1000D); // get connection time elapsed(from first request) in seconds, under the number of requests.
		if (rqps > maxRequestPerSecond) { // if the average req/sec if >= the max,
			return (int)(returnWeight * (rqps / (double)maxRequestPerSecond)); // return the amount over, will usually be 1*returnWeight. ex. if they got 128 rq/s, it would return 2*returnWeight by default
		}
		return 0;
	}
}
