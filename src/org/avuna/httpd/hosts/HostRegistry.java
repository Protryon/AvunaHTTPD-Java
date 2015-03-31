package org.avuna.httpd.hosts;

import java.util.HashMap;
import org.avuna.httpd.util.Logger;

public class HostRegistry {
	private static final HashMap<Protocol, Class<? extends Host>> ph = new HashMap<Protocol, Class<? extends Host>>();
	
	public static void addHost(Protocol p, Class<? extends Host> host) {
		ph.put(p, host);
	}
	
	public static void unpack() {
		for (Class<? extends Host> host : ph.values()) {
			try {
				host.getDeclaredMethod("unpack", null).invoke(null, null);
			}catch (Exception e) {
				Logger.logError(e);
			}
		}
	}
	
	public static Class<? extends Host> getHost(Protocol p) {
		return ph.get(p);
	}
}
