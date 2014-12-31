package com.javaprophet.javawebserver.http;

import com.javaprophet.javawebserver.networking.Packet;

public class MessageBody {
	private Resource body = null;
	private Packet superPacket = null;
	
	public MessageBody clone(Packet newSuper) {
		MessageBody n = new MessageBody(newSuper, body != null ? body.clone() : null);
		return n;
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
	
	public MessageBody(Packet superPacket, Resource body) {
		this(superPacket);
		this.body = body;
	}
	
	public MessageBody(Packet superPacket) {
		this.superPacket = superPacket;
	}
	
	public Resource getBody() {
		return body;
	}
	
	public void setBody(Resource body) {
		this.body = body;
	}
	
	public String toString() {
		return new String(body.data);
	}
}
