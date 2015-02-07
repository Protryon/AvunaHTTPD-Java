package com.javaprophet.javawebserver.http;

import com.javaprophet.javawebserver.networking.Packet;

/**
 * Provides all the resources for the message body.
 */
public class MessageBody {

    /**
     * The body content such as html/php/java and such.
     */
	private Resource body = null;

    /**
     * Something with reference control
     * TODO: More docs about this
     */
	private Packet superPacket = null;

    /**
     * Clone this object into another object.
     * @param newSuper the reference control
     * @return the cloned object.
     */
	public MessageBody clone(Packet newSuper) {
		MessageBody n = new MessageBody(newSuper, body != null ? body.clone() : null);
		return n;
	}

    /**
     * Constructor for MessageBody that does some setting.
     * @param superPacket the reference control or something.
     * @param body the main content.
     */
	public MessageBody(Packet superPacket, Resource body) {
		this(superPacket);
		this.body = body;
	}

    /**
     * Constructor setting the super packet
     * @param superPacket the super packet.
     */
	public MessageBody(Packet superPacket) {
		this.superPacket = superPacket;
	}

    /**
     * Get the main body content
     * @return the content of the page.
     */
	public Resource getBody() {
		return body;
	}

    /**
     * Set the body/the website content
     * @param body the website content
     */
	public void setBody(Resource body) {
		this.body = body;
	}

    /**
     * toString for Resource
     * @return the main body content.
     */
	public String toString() {
		return new String(body.data);
	}
}
