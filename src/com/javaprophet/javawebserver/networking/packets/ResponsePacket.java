package com.javaprophet.javawebserver.networking.packets;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.http.MessageBody;
import com.javaprophet.javawebserver.http.Method;
import com.javaprophet.javawebserver.http.Resource;
import com.javaprophet.javawebserver.networking.Packet;
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
		n.reqTransfer = reqTransfer;
		return n;
	}
	
	public byte[] cachedSerialize = null;
	public ResponsePacket cachedPacket = null;
	
	public byte[] serialize(boolean data) {
		try {
			long ps1 = System.nanoTime();
			ResponsePacket thisClone = clone();
			byte[] finalc = thisClone.body == null ? null : (thisClone.body.getBody() == null ? null : (thisClone.body.getBody().data));
			long ps2 = System.nanoTime();
			finalc = JavaWebServer.patchBus.processResponse(thisClone, thisClone.request, finalc);
			if (thisClone.drop) {
				return null;
			}
			long start = System.nanoTime();
			StringBuilder ser = new StringBuilder();
			ser.append((thisClone.httpVersion + " " + thisClone.statusCode + " " + thisClone.reasonPhrase + JavaWebServer.crlf));
			HashMap<String, ArrayList<String>> hdrs = thisClone.headers.getHeaders();
			for (String key : hdrs.keySet()) {
				for (String val : hdrs.get(key)) {
					ser.append((key + ": " + val + JavaWebServer.crlf));
				}
			}
			ser.append(JavaWebServer.crlf);
			cachedSerialize = ser.toString().getBytes();
			byte[] add = null;
			if (data && finalc != null) {
				if (!thisClone.headers.hasHeader("Transfer-Encoding")) {
					add = finalc;
				}else {
					cachedPacket = thisClone;
					cachedSerialize = new byte[0];
					return new byte[0];
				}
				if (thisClone.body == null) {
					thisClone.body = new MessageBody(thisClone);
				}
				thisClone.body.setBody(new Resource(finalc, thisClone.headers.hasHeader("Content-Type") ? thisClone.headers.getHeader("Content-Type") : "text/html", thisClone.request.target));
			}else {
				add = JavaWebServer.crlf.getBytes();
			}
			byte[] total = new byte[cachedSerialize.length + add.length];
			System.arraycopy(cachedSerialize, 0, total, 0, cachedSerialize.length);
			System.arraycopy(add, 0, total, cachedSerialize.length, add.length);
			cachedPacket = thisClone;
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
	
	public ResponsePacket write(DataOutputStream out) throws IOException {
		long start = System.nanoTime();
		byte[] write = serialize(request.method != Method.HEAD);
		long as = System.nanoTime();
		cachedPacket.bwt = System.nanoTime();
		if (write == null) {
			return null;
		}else if (write.length == 0) {
			
		}else {
			out.write(write);
			write = null;
			out.flush();
		}
		long pw = System.nanoTime();
		if (cachedPacket.headers.hasHeader("Transfer-Encoding")) {
			String te = cachedPacket.headers.getHeader("Transfer-Encoding");
			if (te.equals("chunked")) {
				cachedPacket.reqTransfer = true;
			}
		}
		if (headers.hasHeader("Connection")) {
			String c = headers.getHeader("Connection");
			if (c.equals("Close")) {
				out.close();
			}
		}
		long ret = System.nanoTime();
		// System.out.println((as - start) / 1000000D + " start-as");
		// System.out.println((pw - as) / 1000000D + " as-pw");
		// System.out.println((ret - pw) / 1000000D + " pw-ret");
		return cachedPacket;
	}
}
