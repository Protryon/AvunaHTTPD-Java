package com.javaprophet.javawebserver.plugins.javaloader;

import java.io.IOException;
import com.javaprophet.javawebserver.http.networking.ChunkedOutputStream;
import com.javaprophet.javawebserver.http.networking.RequestPacket;
import com.javaprophet.javawebserver.http.networking.ResponsePacket;

public abstract class JavaLoaderStream extends JavaLoader {
	public abstract void generate(ChunkedOutputStream out, RequestPacket request, ResponsePacket response) throws IOException;
	
	public final int getType() {
		return 2;
	}
}
