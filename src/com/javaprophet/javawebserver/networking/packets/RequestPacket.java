package com.javaprophet.javawebserver.networking.packets;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.hosts.Host;
import com.javaprophet.javawebserver.http.Headers;
import com.javaprophet.javawebserver.http.MessageBody;
import com.javaprophet.javawebserver.http.Method;
import com.javaprophet.javawebserver.http.Resource;
import com.javaprophet.javawebserver.networking.Packet;
import com.javaprophet.javawebserver.util.Config;
import com.javaprophet.javawebserver.util.Logger;

public class RequestPacket extends Packet {
	public String target = "/";
	public Method method = Method.GET;
	public String userIP = "";
	public int userPort = 80;
	public boolean ssl = false;
	public Config overrideConfig = null;
	public Host host = null;
	// javaloader vars
	public HashMap<String, String> get = new HashMap<String, String>();
	public HashMap<String, String> post = new HashMap<String, String>();
	public HashMap<String, String> cookie = new HashMap<String, String>();
	
	public void procJL() throws UnsupportedEncodingException {
		String get = target.contains("?") ? target.substring(target.indexOf("?") + 1) : "";
		for (String kd : get.split("&")) {
			if (kd.contains("=")) {
				this.get.put(URLDecoder.decode(kd.substring(0, kd.indexOf("=")), "UTF-8"), URLDecoder.decode(kd.substring(kd.indexOf("=") + 1), "UTF-8"));
			}else {
				this.get.put(URLDecoder.decode(kd, "UTF-8"), "");
			}
		}
		if (method == Method.POST && headers.getHeader("Content-Type").startsWith("application/x-www-form-urlencoded") && body != null && body.getBody() != null) {
			String post = new String(body.getBody().data);
			for (String kd : post.split("&")) {
				if (kd.contains("=")) {
					this.post.put(URLDecoder.decode(kd.substring(0, kd.indexOf("=")), "UTF-8"), URLDecoder.decode(kd.substring(kd.indexOf("=") + 1), "UTF-8"));
				}else {
					this.post.put(URLDecoder.decode(kd, "UTF-8"), "");
				}
			}
		}
		if (headers.hasHeader("Cookie")) {
			String cookie = headers.getHeader("Cookie");
			for (String kd : cookie.split(";")) {
				if (kd.contains("=")) {
					this.cookie.put(URLDecoder.decode(kd.substring(0, kd.indexOf("=")), "UTF-8").trim(), URLDecoder.decode(kd.substring(kd.indexOf("=") + 1), "UTF-8").trim());
				}else {
					this.cookie.put(URLDecoder.decode(kd, "UTF-8").trim(), "");
				}
			}
		}
	}
	
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
		if (b == -1) {
			return null;
		}
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
			String[] tenc = headers.getHeader("Transfer-Encoding").split(", ");
			if (tenc[tenc.length - 1].equals("chunked")) {
				chunked = true;
			}
		}
		byte[] bbody = new byte[0];
		if (chunked && htc) {
			String te = headers.getHeader("Transfer-Encoding");
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
			if (te.equals("chunked")) {
				headers.removeHeaders("Transfer-Encoding");
			}else {
				String ntev = "";
				for (String sp : te.split(",")) {
					if (!sp.equals("chunked")) {
						ntev = sp + ", ";
					}
				}
				if (ntev.length() > 0) {
					ntev = ntev.substring(0, ntev.length() - 2);
				}
				te = ntev;
			}
		}else if (hcl) {
			bbody = new byte[Integer.parseInt(headers.getHeader("Content-Length"))];
			in.readFully(bbody);
		}
		MessageBody body = new MessageBody(incomingRequest, new Resource(bbody, headers.hasHeader("Content-Type") ? headers.getHeader("Content-Type") : "application/octet-stream"));
		incomingRequest.body = body;
		return incomingRequest;
	}
	
	public byte[] serialize() {
		try {
			ByteArrayOutputStream ser = new ByteArrayOutputStream();
			ser.write((method.name + " " + target + " " + httpVersion + JavaWebServer.crlf).getBytes());
			HashMap<String, ArrayList<String>> hdrs = headers.getHeaders();
			for (String key : hdrs.keySet()) {
				for (String val : hdrs.get(key)) {
					ser.write((key + ": " + val + JavaWebServer.crlf).getBytes());
				}
			}
			ser.write(JavaWebServer.crlf.getBytes());
			ser.write(body.getBody().data);
			return ser.toByteArray();
		}catch (Exception e) {
			Logger.logError(e);
		}
		return new byte[0];
	}
	
	public String toString() {
		return new String(serialize());
	}
}
