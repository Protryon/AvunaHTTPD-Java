package com.javaprophet.javawebserver.http.networking;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.http.Method;
import com.javaprophet.javawebserver.http.Resource;
import com.javaprophet.javawebserver.plugins.javaloader.JavaLoaderStream;
import com.javaprophet.javawebserver.util.Logger;

/**
 * This is for responeses to the client.
 */
public class ResponsePacket extends Packet {
	public int statusCode = 200;
	public String reasonPhrase = "";
	public RequestPacket request;
	public JavaLoaderStream reqStream = null;
	public boolean done = false;
	
	public byte[] serialize() {
		return serialize(true, true);
	}
	
	// public ResponsePacket clone() {
	// ResponsePacket n = new ResponsePacket();
	// n.statusCode = statusCode;
	// n.reasonPhrase = reasonPhrase;
	// n.request = request;
	// n.httpVersion = httpVersion;
	// n.headers = headers.clone();
	// n.body = body.clone();
	// n.reqTransfer = reqTransfer;
	// return n;
	// }
	
	public byte[] cachedSerialize = null;
	public ResponsePacket cachedPacket = null;
	
	public byte[] serialize(boolean data, boolean head) {
		try {
			long ps1 = System.nanoTime();
			// ResponsePacket thisClone = clone();
			byte[] finalc = this.body == null ? null : this.body.data;
			long ps2 = System.nanoTime();
			finalc = JavaWebServer.patchBus.processResponse(this, this.request, finalc);
			if (this.drop) {
				return null;
			}
			long start = System.nanoTime();
			StringBuilder ser = new StringBuilder();
			if (head) {
				ser.append((this.httpVersion + " " + this.statusCode + " " + this.reasonPhrase + JavaWebServer.crlf));
				HashMap<String, ArrayList<String>> hdrs = this.headers.getHeaders();
				for (String key : hdrs.keySet()) {
					for (String val : hdrs.get(key)) {
						ser.append((key + ": " + val + JavaWebServer.crlf));
					}
				}
				ser.append(JavaWebServer.crlf);
			}
			cachedSerialize = ser.toString().getBytes();
			byte[] add = null;
			if (data && finalc != null) {
				if (!this.headers.hasHeader("Transfer-Encoding")) {
					add = finalc;
				}else {
					// cachedPacket = this;
					cachedSerialize = new byte[0];
					return new byte[0];
				}
				this.body = new Resource(finalc, this.headers.hasHeader("Content-Type") ? this.headers.getHeader("Content-Type") : "text/html", this.request.target, this.body.effectiveOverride);
			}else {
				add = JavaWebServer.crlf.getBytes();
			}
			byte[] total = new byte[cachedSerialize.length + add.length];
			System.arraycopy(cachedSerialize, 0, total, 0, cachedSerialize.length);
			System.arraycopy(add, 0, total, cachedSerialize.length, add.length);
			// cachedPacket = this;
			long end = System.nanoTime();
			// System.out.println("serialize: " + ((end - start) / 1000000D) + ", " + ((start - ps2) / 1000000D) + ", " + ((ps2 - ps1) / 1000000D) + " ms");
			return total;
		}catch (Exception e) {
			Logger.logError(e);
		}
		cachedSerialize = new byte[0];
		return new byte[0];
	}
	
	public String toString() {
		return new String(cachedSerialize);
	}
	
	public long bwt = 0L;
	public boolean reqTransfer = false;
	
	public byte[] subwrite = null;
	public boolean validSub = true;
	public boolean close = false;
	
	public void prewrite() {
		subwrite = serialize(request.method != Method.HEAD, true);
		if (this.headers.hasHeader("Transfer-Encoding")) {
			String te = this.headers.getHeader("Transfer-Encoding");
			if (te.equals("chunked")) {
				subwrite = null;
				validSub = false;
			}
		}
		this.bwt = System.nanoTime();
		if (this.headers.hasHeader("Transfer-Encoding")) {
			String te = this.headers.getHeader("Transfer-Encoding");
			if (te.equals("chunked")) {
				this.reqTransfer = true;
			}
		}
		if (headers.hasHeader("Connection")) {
			String c = headers.getHeader("Connection");
			if (c.equals("Close")) {
				close = true;
			}
		}
	}
	
	public void subwrite() {
		subwrite = serialize(true, false);
		if (this.headers.hasHeader("Transfer-Encoding")) {
			String te = this.headers.getHeader("Transfer-Encoding");
			if (te.equals("chunked")) {
				subwrite = null;
				validSub = false;
			}
		}
		this.bwt = System.nanoTime();
	}
	
	public void write(DataOutputStream out, boolean deprecated) throws IOException {
		long start = System.nanoTime();
		byte[] write = serialize(request.method != Method.HEAD, true);
		long as = System.nanoTime();
		this.bwt = System.nanoTime();
		if (write == null) {
			return;
		}else if (write.length == 0) {
			
		}else {
			out.write(write);
			write = null;
			out.flush();
		}
		long pw = System.nanoTime();
		
		long ret = System.nanoTime();
		// Logger.log((as - start) / 1000000D + " start-as");
		// Logger.log((pw - as) / 1000000D + " as-pw");
		// Logger.log((ret - pw) / 1000000D + " pw-ret");
	}
}
