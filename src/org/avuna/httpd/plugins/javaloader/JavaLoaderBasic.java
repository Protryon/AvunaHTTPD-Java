package org.avuna.httpd.plugins.javaloader;

import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;

public abstract class JavaLoaderBasic extends JavaLoader {
	
	public abstract byte[] generate(ResponsePacket response, RequestPacket request);
	
	public final int getType() {
		return 0;
	}
	
}
