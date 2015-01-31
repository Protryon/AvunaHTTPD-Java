package com.javaprophet.javawebserver.plugins.javaloader;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;

public abstract class JavaLoaderStream extends JavaLoader {
	
	@Override
	public byte[] generate(ResponsePacket response, RequestPacket request) {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(bout);
		generate(out, response, request);
		return bout.toByteArray();
	}
	
	public abstract void generate(PrintStream out, ResponsePacket response, RequestPacket request);
	
}
