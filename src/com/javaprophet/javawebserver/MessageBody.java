package com.javaprophet.javawebserver;

public class MessageBody {
	private byte[] body = new byte[0];
	private Packet superPacket = null;
	
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
		if (superPacket.headers.hasHeader("Content-Length")) {
			superPacket.headers.getHeader("Content-Length").value = body.length + "";
		}else {
			superPacket.headers.addHeader("Content-Length", body.length + "");
		}
	}
	
	public String toString() {
		return new String(body);
	}
}
