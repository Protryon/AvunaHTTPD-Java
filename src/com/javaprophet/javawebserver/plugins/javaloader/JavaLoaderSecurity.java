package com.javaprophet.javawebserver.plugins.javaloader;

import com.javaprophet.javawebserver.networking.packets.RequestPacket;

public abstract class JavaLoaderSecurity extends JavaLoader {
	public abstract int check(RequestPacket request);
	
	public final int getType() {
		return 3;
	}
}
