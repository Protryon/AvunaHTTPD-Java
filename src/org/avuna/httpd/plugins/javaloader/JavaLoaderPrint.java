package org.avuna.httpd.plugins.javaloader;

import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;

public abstract class JavaLoaderPrint extends JavaLoader {
	
	public abstract boolean generate(HTMLBuilder out, ResponsePacket response, RequestPacket request);
	
	public final int getType() {
		return 1;
	}
	
}
