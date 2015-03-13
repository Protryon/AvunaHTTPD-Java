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
import com.javaprophet.javawebserver.hosts.VHost;
import com.javaprophet.javawebserver.http.Headers;
import com.javaprophet.javawebserver.http.Method;
import com.javaprophet.javawebserver.http.Resource;
import com.javaprophet.javawebserver.networking.Work;
import com.javaprophet.javawebserver.util.Logger;

public class RequestPacket extends Packet {
	public String target = "/";
	public Method method = Method.GET;
	public String userIP = "";
	public int userPort = 80;
	public boolean ssl = false;
	public VHost host = null;
	public boolean forbode = false;
	public String oredir = "";
	public String[] overrideIndex = null;
	public String overrideType = null;
	public int overrideCache = -2;
	public Work work = null;
	public RequestPacket parent = null;
	public ResponsePacket child = null;
	public int order = -1;
	// javaloader vars
	public HashMap<String, String> get = new HashMap<String, String>();
	public HashMap<String, String> post = new HashMap<String, String>();
	public HashMap<String, String> cookie = new HashMap<String, String>();
	
	public RequestPacket clone() {
		RequestPacket ret = new RequestPacket();
		ret.headers = headers.clone();
		ret.drop = drop;
		ret.body = body.clone();
		ret.httpVersion = httpVersion;
		ret.target = target;
		ret.method = method;
		ret.userIP = userIP;
		ret.userPort = userPort;
		ret.ssl = ssl;
		ret.host = host;
		ret.forbode = forbode;
		ret.oredir = oredir;
		ret.overrideIndex = overrideIndex;
		ret.overrideType = overrideType;
		ret.overrideCache = overrideCache;
		ret.parent = parent;
		ret.work = work;
		ret.get = get;
		ret.post = post;
		ret.cookie = cookie;
		return ret;
	}
	
	public void procJL() throws UnsupportedEncodingException {
		String get = target.contains("?") ? target.substring(target.indexOf("?") + 1) : "";
		for (String kd : get.split("&")) {
			if (kd.contains("=")) {
				this.get.put(URLDecoder.decode(kd.substring(0, kd.indexOf("=")), "UTF-8"), URLDecoder.decode(kd.substring(kd.indexOf("=") + 1), "UTF-8"));
			}else {
				this.get.put(URLDecoder.decode(kd, "UTF-8"), "");
			}
		}
		if (method == Method.POST && headers.getHeader("Content-Type").startsWith("application/x-www-form-urlencoded") && body != null) {
			String post = new String(body.data);
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
	
	private static String readLine(DataInputStream in) throws IOException {
		ByteArrayOutputStream writer = new ByteArrayOutputStream();
		int i = in.read();
		while (i != JavaWebServer.crlfb[0] && i != -1) {
			writer.write(i);
			i = in.read();
		}
		if (JavaWebServer.crlfb.length == 2) in.read();
		return writer.toString();
	}
	
	public static RequestPacket read(DataInputStream in) throws IOException {
		long start = System.nanoTime();
		RequestPacket incomingRequest = new RequestPacket();
		String reqLine = "";
		long pir = System.nanoTime();
		reqLine = readLine(in).trim();
		int b = reqLine.indexOf(" ");
		if (b == -1) {
			return null;
		}
		long rlr = System.nanoTime();
		incomingRequest.method = Method.get(reqLine.substring(0, b));
		incomingRequest.target = reqLine.substring(b + 1, b = reqLine.indexOf(" ", b + 1));
		incomingRequest.httpVersion = reqLine.substring(b + 1);
		long ss = System.nanoTime();
		Headers headers = incomingRequest.headers;
		int hdr = 0;
		while (true) {
			String headerLine = readLine(in);
			if (headerLine.length() == 0) {
				break;
			}else {
				headers.addHeader(headerLine);
				hdr++;
			}
			long he = System.nanoTime();
			if (hdr > 100) {
				break;
			}
		}
		long ph = System.nanoTime();
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
			// TODO: if needed, reimplement properly
			
		}else if (hcl) {
			bbody = new byte[Integer.parseInt(headers.getHeader("Content-Length"))];
			in.readFully(bbody);
		}
		long pb = System.nanoTime();
		incomingRequest.body = new Resource(bbody, headers.hasHeader("Content-Type") ? headers.getHeader("Content-Type") : "application/octet-stream");
		long cur = System.nanoTime();
		// Logger.log((pir - start) / 1000000D + " start-pir");
		// Logger.log((rlr - pir) / 1000000D + " pir-rlr");
		// Logger.log((ss - rlr) / 1000000D + " rlr-ss");
		// Logger.log((ph - ss) / 1000000D + " ss-ph");
		// Logger.log((pb - ph) / 1000000D + " ph-pb");
		// Logger.log((cur - pb) / 1000000D + " pb-cur");
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
			ser.write(body.data);
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
