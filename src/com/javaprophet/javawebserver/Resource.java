package com.javaprophet.javawebserver;

public class Resource {
	public byte[] data = new byte[0];
	public String type = "text/html";
	
	public Resource(byte[] data, String type) {
		this.data = data;
		this.type = type;
	}
}
