package com.javaprophet.javawebserver.http;

public class Resource {
	public byte[] data = new byte[0];
	public String type = "text/html";
	public String loc = "/";
	
	public Resource(byte[] data, String type) {
		this.data = data;
		this.type = type;
	}
	
	public Resource(byte[] data, String type, String loc) {
		this.data = data;
		this.type = type;
		this.loc = loc;
	}
}
