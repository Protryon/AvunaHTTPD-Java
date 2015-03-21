package com.javaprophet.javawebserver.plugins.javaloader;

import com.javaprophet.javawebserver.http.networking.RequestPacket;

public abstract class JavaLoaderSecurity extends JavaLoader {
	public abstract int check(RequestPacket request);
	
	public abstract int check(String ip);
	
	public final int getType() {
		return 3;
	}
}
