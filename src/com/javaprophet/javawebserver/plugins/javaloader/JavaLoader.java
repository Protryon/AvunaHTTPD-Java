package com.javaprophet.javawebserver.plugins.javaloader;

import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;

public abstract class JavaLoader {
	public JavaLoader() {
		
	}
	
	public void destroy() {
		
	}
	
	public abstract byte[] generate(ResponsePacket response, RequestPacket request);
}
