package com.javaprophet.javawebserver.http.networking;

import com.javaprophet.javawebserver.http.Headers;
import com.javaprophet.javawebserver.http.Resource;

public class Packet {
	public String httpVersion = "HTTP/1.1";
	public Headers headers = new Headers();
	public Resource body = null;
	public boolean drop = false;
	
	public String toString() {
		return "";
	}
}
