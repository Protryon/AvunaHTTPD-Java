package com.javaprophet.javawebserver;

public class Packet {
	public String httpVersion = "HTTP/1.1";
	public Headers headers = new Headers();
	public MessageBody body = new MessageBody(this);
	public static final String crlf = System.getProperty("line.separator");
	
	public String toString() {
		return "";
	}
}
