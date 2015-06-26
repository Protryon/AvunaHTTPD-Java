/*
 * Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.http.Method;
import org.avuna.httpd.http.plugins.javaloader.JavaLoaderStream;
import org.avuna.httpd.util.Logger;

/** This is for responeses to the client. */
public class ResponsePacket extends Packet {
	public int statusCode = 200;
	public String reasonPhrase = "";
	public RequestPacket request;
	public JavaLoaderStream reqStream = null;
	public boolean done = false;
	public DataInputStream toStream = null;
	
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
			if (this.drop) {
				return null;
			}
			StringBuilder ser = new StringBuilder();
			if (head) {
				ser.append((this.httpVersion + " " + this.statusCode + " " + this.reasonPhrase + AvunaHTTPD.crlf));
				HashMap<String, String[]> hdrs = this.headers.getHeaders();
				for (String key : hdrs.keySet()) {
					for (String val : hdrs.get(key)) {
						ser.append((key + ": " + val + AvunaHTTPD.crlf));
					}
				}
				ser.append(AvunaHTTPD.crlf);
			}
			cachedSerialize = ser.toString().getBytes();
			byte[] add = null;
			if (data && body != null) {
				if (!this.headers.hasHeader("Transfer-Encoding")) {
					add = body.data;
				}else {
					// cachedPacket = this;
					cachedSerialize = new byte[0];
					return new byte[0];
				}
			}else {
				add = new byte[0];// AvunaHTTPD.crlf.getBytes();
			}
			byte[] total = new byte[cachedSerialize.length + add.length];
			System.arraycopy(cachedSerialize, 0, total, 0, cachedSerialize.length);
			System.arraycopy(add, 0, total, cachedSerialize.length, add.length);
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
		byte[] write = serialize(request.method != Method.HEAD, true);
		this.bwt = System.nanoTime();
		if (write == null) {
			return;
		}else if (write.length == 0) {
			
		}else {
			out.write(write);
			write = null;
			out.flush();
		}
	}
}
