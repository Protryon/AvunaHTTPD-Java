package org.avuna.httpd.plugins.javaloader;

import org.avuna.httpd.http.networking.RequestPacket;

public abstract class JavaLoaderSecurity extends JavaLoader {
	public abstract int check(RequestPacket request);
	
	public abstract int check(String ip);
	
	public final int getType() {
		return 3;
	}
}
