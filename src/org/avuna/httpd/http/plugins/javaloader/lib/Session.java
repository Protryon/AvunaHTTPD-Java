package org.avuna.httpd.http.plugins.javaloader.lib;

import java.util.HashMap;
import java.util.UUID;
import org.avuna.httpd.http.networking.ResponsePacket;

public class Session {
	private final String csessid;
	private final String useragent;
	private final String ip;
	private final boolean bindIP, bindAgent;
	private final HashMap<String, String> data = new HashMap<String, String>();
	
	protected Session(ResponsePacket base, boolean bindIP, boolean bindAgent) {
		this.csessid = UUID.randomUUID().toString();
		this.useragent = base.request.headers.hasHeader("User-Agent") ? base.request.headers.getHeader("User-Agent") : "";
		this.ip = base.request.userIP;
		this.bindAgent = bindAgent;
		this.bindIP = bindIP;
		base.headers.addHeader("Set-Cookie", "jSessID=" + csessid);
	}
	
	private static final HashMap<String, Session> sesses = new HashMap<String, Session>();
	
	public static Session getSession(ResponsePacket base, boolean bindIP, boolean bindAgent) {
		if (base.request.cookie.containsKey("jSessID")) {
			Session maybe = sesses.get(base.request.cookie.get("jSessID"));
			if (maybe != null) {
				if (!maybe.bindIP || maybe.ip.equals(base.request.userIP)) {
					if (!maybe.bindAgent || maybe.useragent.equals(base.request.headers.hasHeader("User-Agent") ? base.request.headers.getHeader("User-Agent") : "")) {
						return maybe;
					}
				}
			}
		}
		Session n = new Session(base, bindIP, bindAgent);
		sesses.put(n.csessid, n);
		return n;
	}
	
	public static Session getSession(ResponsePacket base) {
		return getSession(base, true, true);
	}
	
	public String get(String name) {
		return data.get(name);
	}
	
	public boolean has(String name) {
		return data.containsKey(name);
	}
	
	public Session set(String name, String value) {
		data.put(name, value);
		return this;
	}
}
