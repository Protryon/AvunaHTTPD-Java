package com.javaprophet.javawebserver.plugins.javaloader;

import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;

public abstract class JavaLoaderStream extends JavaLoader {
	public abstract void generate(ChunkedOutputStream out, RequestPacket request, ResponsePacket response);
	
	public final int getType() {
		return 2;
	}
}
