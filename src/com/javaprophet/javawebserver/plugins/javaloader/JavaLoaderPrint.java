package com.javaprophet.javawebserver.plugins.javaloader;

import com.javaprophet.javawebserver.http.networking.RequestPacket;
import com.javaprophet.javawebserver.http.networking.ResponsePacket;

public abstract class JavaLoaderPrint extends JavaLoader {
	
	public abstract boolean generate(HTMLBuilder out, ResponsePacket response, RequestPacket request);
	
	public final int getType() {
		return 1;
	}
	
}
