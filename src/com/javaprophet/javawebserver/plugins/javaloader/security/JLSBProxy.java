package com.javaprophet.javawebserver.plugins.javaloader.security;

import com.javaprophet.javawebserver.http.Method;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.plugins.javaloader.JavaLoaderSecurity;

public class JLSBProxy extends JavaLoaderSecurity {
	
	@Override
	public int check(RequestPacket req) {
		if (req.method == Method.POST) {
			if (req.headers.hasHeader("Content-Type")) {
				if (req.headers.getHeader("Content-Type").startsWith("application/x-www-form-urlencoded")) {
					String body = new String(req.body.data);
					if (!body.contains("=")) {
						return 100;
					}
				}
			}else {
				return 100;
			}
		}
		return 0;
	}
	
	@Override
	public int check(String arg0) {
		return 0;
	}
	
}
