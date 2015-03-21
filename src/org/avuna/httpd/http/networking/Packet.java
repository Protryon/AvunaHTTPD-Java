package org.avuna.httpd.http.networking;

import org.avuna.httpd.http.Headers;
import org.avuna.httpd.http.Resource;

public class Packet {
	public String httpVersion = "HTTP/1.1";
	public Headers headers = new Headers();
	public Resource body = null;
	public boolean drop = false;
	
	public String toString() {
		return "";
	}
}
