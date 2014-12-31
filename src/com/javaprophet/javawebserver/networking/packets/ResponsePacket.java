package com.javaprophet.javawebserver.networking.packets;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.http.Header;
import com.javaprophet.javawebserver.http.Method;
import com.javaprophet.javawebserver.networking.Packet;

/**
 * This is for responeses to the client.
 */
public class ResponsePacket extends Packet {
	public int statusCode = 200;
	public String reasonPhrase = "";
	public RequestPacket request;
	
	public byte[] serialize() {
		return serialize(true);
	}
	
	public ResponsePacket clone() {
		ResponsePacket n = new ResponsePacket();
		n.statusCode = statusCode;
		n.reasonPhrase = reasonPhrase;
		n.request = request;
		n.httpVersion = httpVersion;
		n.headers = headers.clone();
		n.body = body.clone(n);
		return n;
	}
	
	public byte[] serialize(boolean data) {
		try {
			ResponsePacket thisClone = clone();
			byte[] finalc = thisClone.body == null ? null : (thisClone.body.getBody() == null ? null : (thisClone.body.getBody().data));
			finalc = JavaWebServer.patchBus.processResponse(thisClone, thisClone.request, finalc);
			ByteArrayOutputStream ser = new ByteArrayOutputStream();
			ser.write((thisClone.httpVersion + " " + thisClone.statusCode + " " + thisClone.reasonPhrase + crlf).getBytes());
			for (Header header : thisClone.headers.getHeaders()) {
				ser.write((header.toLine() + crlf).getBytes());
			}
			ser.write(crlf.getBytes());
			if (data && finalc != null) ser.write(finalc);
			return ser.toByteArray();
		}catch (Exception e) {
			e.printStackTrace();
		}
		return new byte[0];
	}
	
	public String toString() {
		return new String(serialize());
	}
	
	public String toString2() {
		return new String(serialize(false));
	}
	
	public void write(DataOutputStream out) throws IOException {
		out.write(serialize(request.method != Method.HEAD));
		out.flush();
		if (headers.hasHeader("Connection")) {
			String c = headers.getHeader("Connection").value;
			if (c.equals("Close")) {
				out.close();
			}
		}
	}
}
