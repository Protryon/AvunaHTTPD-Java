package com.javaprophet.javawebserver.plugins.javaloader.security;

import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.plugins.javaloader.JavaLoaderSecurity;

public class JLSFlood extends JavaLoaderSecurity {
	
	@Override
	public int check(RequestPacket req) {
		if (req.target.matches("/\\?[0-9a-zA-Z]{2,}")) {
			return 100;
		}
		return 0;
	}
	
	@Override
	public int check(String arg0) {
		return 0;
	}
	
}
