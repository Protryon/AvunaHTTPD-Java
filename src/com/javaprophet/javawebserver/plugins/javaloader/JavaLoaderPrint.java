package com.javaprophet.javawebserver.plugins.javaloader;

import java.io.PrintStream;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;

public abstract class JavaLoaderPrint extends JavaLoader {
	
	public abstract void generate(PrintStream out, ResponsePacket response, RequestPacket request);
	
}
