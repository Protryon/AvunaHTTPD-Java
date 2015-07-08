package org.avuna.httpd.http.plugins.security;

import org.avuna.httpd.http.Headers;
import org.avuna.httpd.http.Method;
import org.avuna.httpd.http.Resource;
import org.avuna.httpd.http.networking.RequestPacket;

public class RequestPacketShell {
	public final Method method;
	public final String path;
	public final String version;
	public final Headers headers;
	public final Resource resource;
	public final long when = System.nanoTime();
	
	public RequestPacketShell(RequestPacket request) {
		this.method = request.method;
		this.path = request.target;
		this.version = request.httpVersion;
		this.headers = request.headers;
		this.resource = request.body;
	}
}
