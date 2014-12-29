package com.javaprophet.javawebserver;

public class MessageBody {
	private byte[] body = new byte[0];
	private Packet superPacket = null;
	private String contentType = "text/html";
	
	public String getContentType() {
		return contentType;
	}
	
	public void setContentType(String ct) {
		this.contentType = ct;
	}
	
	public ContentEncoding getContentEncoding() {
		return ContentEncoding.get(superPacket.headers.hasHeader("Content-Encoding") ? superPacket.headers.getHeader("Content-Encoding").value : "");
	}
	
	public void setContentEncoding(ContentEncoding ce) {
		if (superPacket.headers.hasHeader("Content-Encoding")) {
			if (ce == ContentEncoding.identity) {
				superPacket.headers.removeHeaders("Content-Encoding");
			}else {
				superPacket.headers.getHeader("Content-Encoding").value = ce.name;
			}
		}else {
			superPacket.headers.addHeader("Content-Encoding", ce.name);
		}
	}
	
	public MessageBody(Packet superPacket, byte[] body) {
		this(superPacket);
		this.body = body;
	}
	
	public MessageBody(Packet superPacket) {
		this.superPacket = superPacket;
	}
	
	public byte[] getBody() {
		return body;
	}
	
	public void setBody(byte[] body) {
		this.body = body;
	}
	
	public String toString() {
		return new String(body);
	}
}
