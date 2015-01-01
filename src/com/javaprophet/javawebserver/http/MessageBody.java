package com.javaprophet.javawebserver.http;

import com.javaprophet.javawebserver.networking.Packet;

public class MessageBody {
	private Resource body = null;
	private Packet superPacket = null;
	
	public MessageBody clone(Packet newSuper) {
		MessageBody n = new MessageBody(newSuper, body != null ? body.clone() : null);
		return n;
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
