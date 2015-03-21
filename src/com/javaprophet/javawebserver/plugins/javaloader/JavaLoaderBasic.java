package com.javaprophet.javawebserver.plugins.javaloader;

import com.javaprophet.javawebserver.http.networking.RequestPacket;
import com.javaprophet.javawebserver.http.networking.ResponsePacket;

public abstract class JavaLoaderBasic extends JavaLoader {
	
	public abstract byte[] generate(ResponsePacket response, RequestPacket request);
	
	public final int getType() {
		return 0;
	}
	
}
