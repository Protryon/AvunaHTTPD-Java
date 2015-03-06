package com.javaprophet.javawebserver.plugins.javaloader.security;

import com.javaprophet.javawebserver.networking.ThreadWorker;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.plugins.javaloader.JavaLoaderSecurity;

public class JLSBFlood extends JavaLoaderSecurity {
	
	@Override
	public int check(String ip) {
		int ips = ThreadWorker.getConnectionsForIP(ip);
		if (ips > 50) {
			return 100;
		}
		return 0;
	}
	
	@Override
	public int check(RequestPacket req) {
		return 0;
	}
}
