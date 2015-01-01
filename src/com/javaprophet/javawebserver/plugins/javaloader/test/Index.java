package com.javaprophet.javawebserver.plugins.javaloader.test;

import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.javaloader.JavaLoader;

public class Index extends JavaLoader {
	
	@Override
	public byte[] generate(ResponsePacket response, RequestPacket request) {
		return "testing un deux trois.".getBytes();
	}
	
}
