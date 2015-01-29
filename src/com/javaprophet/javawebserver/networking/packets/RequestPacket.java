package com.javaprophet.javawebserver.networking.packets;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import com.javaprophet.javawebserver.http.Header;
import com.javaprophet.javawebserver.http.Headers;
import com.javaprophet.javawebserver.http.MessageBody;
import com.javaprophet.javawebserver.http.Method;
import com.javaprophet.javawebserver.http.Resource;
import com.javaprophet.javawebserver.networking.Packet;

public class RequestPacket extends Packet {
	public String target = "/";
	public Method method = Method.GET;
	public String userIP = "";
	public int userPort = 80;
	public boolean ssl = false;
	
	public void write(DataOutputStream out) throws IOException {
		out.write(serialize());
		out.flush();
	}
	
	public static RequestPacket read(DataInputStream in) throws IOException {
		RequestPacket incomingRequest = new RequestPacket();
		String reqLine = "";
		try {
			reqLine = in.readLine().trim();
		}catch (NullPointerException e) {
			return null;
		}
		int b = reqLine.indexOf(" ");
		incomingRequest.method = Method.get(reqLine.substring(0, b));
		incomingRequest.target = reqLine.substring(b + 1, b = reqLine.indexOf(" ", b + 1));
		incomingRequest.httpVersion = reqLine.substring(b + 1);
		Headers headers = incomingRequest.headers;
		while (true) {
			String headerLine = in.readLine().trim();
			if (headerLine.length() == 0) {
				break;
			}else {
				headers.addHeader(headerLine);
			}
		}
		boolean chunked = false;
		boolean htc = headers.hasHeader("Transfer-Encoding");
		boolean hcl = headers.hasHeader("Content-Length");
		if (htc) {
			String[] tenc = headers.getHeader("Transfer-Encoding").value.split(", ");
			if (tenc[tenc.length - 1].equals("chunked")) {
				chunked = true;
			}
		}
		byte[] bbody = new byte[0];
		if (chunked && htc) {
			Header te = headers.getHeader("Transfer-Encoding");
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			boolean lwl = false;
			int length = 1;
			while (length > 0) {
				if (lwl) {
					byte[] data = new byte[length];
					in.readFully(data);
					bout.write(data);
					lwl = false;
				}else {
					String len = in.readLine().trim();
					if (len.equals("")) {
						continue;
					}
					length = Integer.parseInt(len, 16);
					lwl = true;
				}
			}
			bbody = bout.toByteArray();
			if (te.value.equals("chunked")) {
				headers.removeHeaders("Transfer-Encoding");
			}else {
				String ntev = "";
				for (String sp : te.value.split(",")) {
					if (!sp.equals("chunked")) {
						ntev = sp + ", ";
					}
				}
				if (ntev.length() > 0) {
					ntev = ntev.substring(0, ntev.length() - 2);
				}
				te.value = ntev;
			}
		}else if (hcl) {
			bbody = new byte[Integer.parseInt(headers.getHeader("Content-Length").value)];
			in.readFully(bbody);
		}
		MessageBody body = new MessageBody(incomingRequest, new Resource(bbody, headers.hasHeader("Content-Type") ? headers.getHeader("Content-Type").value : "application/octet-stream"));
		incomingRequest.body = body;
		return incomingRequest;
	}
	
	public byte[] serialize() {
		try {
			ByteArrayOutputStream ser = new ByteArrayOutputStream();
			ser.write((method.name + " " + target + " " + httpVersion + crlf).getBytes());
			for (Header header : headers.getHeaders()) {
				ser.write((header.toLine() + crlf).getBytes());
			}
			ser.write(crlf.getBytes());
			ser.write(body.getBody().data);
			return ser.toByteArray();
		}catch (Exception e) {
			e.printStackTrace();
		}
		return new byte[0];
	}
	
	public String toString() {
		return new String(serialize());
	}
}
