package com.javaprophet.javawebserver.hosts;

import java.util.HashMap;

public class HostRegistry {
	private static final HashMap<Protocol, Class<? extends Host>> ph = new HashMap<Protocol, Class<? extends Host>>();
	
	public static void addHost(Protocol p, Class<? extends Host> host) {
		ph.put(p, host);
	}
	
	public static Class<? extends Host> getHost(Protocol p) {
		return ph.get(p);
	}
}
