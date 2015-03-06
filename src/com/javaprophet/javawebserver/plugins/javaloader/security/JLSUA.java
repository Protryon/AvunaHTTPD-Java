package com.javaprophet.javawebserver.plugins.javaloader.security;

import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.plugins.javaloader.JavaLoaderSecurity;

public class JLSUA extends JavaLoaderSecurity {
	
	@Override
	public int check(RequestPacket req) {
		if (!req.headers.hasHeader("User-Agent")) {
			return 100;
		}
		String ua = req.headers.getHeader("User-Agent").toLowerCase().trim();
		if (ua.contains("wordpress") || ua.contains("sql") || ua.contains("php") || ua.contains("scan")) {
			return 100;
		}
		return 0;
	}
	
	@Override
	public int check(String arg0) {
		return 0;
	}
	
}
