package com.javaprophet.javawebserver.networking.packets;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.http.MessageBody;
import com.javaprophet.javawebserver.http.Method;
import com.javaprophet.javawebserver.http.Resource;
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
	
	public byte[] cachedSerialize = null;
	public ResponsePacket cachedPacket = null;
	
	public byte[] serialize(boolean data) {
		try {
			ResponsePacket thisClone = clone();
			byte[] finalc = thisClone.body == null ? null : (thisClone.body.getBody() == null ? null : (thisClone.body.getBody().data));
			finalc = JavaWebServer.patchBus.processResponse(thisClone, thisClone.request, finalc);
			ByteArrayOutputStream ser = new ByteArrayOutputStream();
			ser.write((thisClone.httpVersion + " " + thisClone.statusCode + " " + thisClone.reasonPhrase + crlf).getBytes());
			HashMap<String, ArrayList<String>> hdrs = thisClone.headers.getHeaders();
			for (String key : hdrs.keySet()) {
				for (String val : hdrs.get(key)) {
					ser.write((key + ": " + val + crlf).getBytes());
				}
			}
			ser.write(crlf.getBytes());
			cachedSerialize = ser.toByteArray();
			if (data && finalc != null) {
				ser.write(finalc);
				if (thisClone.body == null) {
					thisClone.body = new MessageBody(thisClone);
				}
				thisClone.body.setBody(new Resource(finalc, thisClone.request.target, thisClone.headers.hasHeader("Content-Type") ? thisClone.headers.getHeader("Content-Type") : "text/html"));
			}else {
				ser.write(crlf.getBytes());
			}
			cachedPacket = thisClone;
			return ser.toByteArray();
		}catch (Exception e) {
			e.printStackTrace();
		}
		cachedSerialize = new byte[0];
		return new byte[0];
	}
	
	public String toString() {
		return new String(cachedSerialize);
	}
	
	public ResponsePacket write(DataOutputStream out) throws IOException {
		out.write(serialize(request.method != Method.HEAD));
		out.flush();
		if (headers.hasHeader("Connection")) {
			String c = headers.getHeader("Connection");
			if (c.equals("Close")) {
				out.close();
			}
		}
		return cachedPacket;
	}
}
