package com.javaprophet.javawebserver.plugins.javaloader;

import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;

public abstract class JavaLoaderPrint extends JavaLoader {
	
	public abstract boolean generate(HTMLBuilder out, ResponsePacket response, RequestPacket request);
	
	public final int getType() {
		return 1;
	}
	
}
