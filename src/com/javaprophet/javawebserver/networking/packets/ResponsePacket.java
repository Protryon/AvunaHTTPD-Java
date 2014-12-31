package com.javaprophet.javawebserver.networking.packets;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.http.ContentEncoding;
import com.javaprophet.javawebserver.http.Header;
import com.javaprophet.javawebserver.http.Headers;
import com.javaprophet.javawebserver.http.MessageBody;
import com.javaprophet.javawebserver.http.Resource;
import com.javaprophet.javawebserver.networking.Packet;

/**
 * This is for responeses to the client.
 */
public class ResponsePacket extends Packet {
	public int statusCode = 200;
	public String reasonPhrase = "";
	public boolean isHead = false;
	public RequestPacket request;
	
	public byte[] serialize(ContentEncoding ce) {
		return serialize(ce, true);
	}
	
	public ResponsePacket clone() {
		ResponsePacket n = new ResponsePacket();
		n.statusCode = statusCode;
		n.reasonPhrase = reasonPhrase;
		n.isHead = isHead;
		n.request = request;
		n.httpVersion = httpVersion;
		n.headers = headers.clone();
		n.body = body.clone(n);
		return n;
	}
	
	public byte[] serialize(ContentEncoding ce, boolean data) {
		try {
			ResponsePacket thisClone = clone();
			byte[] finalc = new byte[0];
			if (thisClone.body != null && thisClone.body.getBody() != null) {
				finalc = thisClone.body.getBody().data;
				if (thisClone.headers.hasHeader("Content-Length") && !thisClone.isHead) {
					thisClone.headers.getHeader("Content-Length").value = thisClone.body.getBody().data.length + "";
				}else if (!thisClone.headers.hasHeader("Transfer-Encoding") || !thisClone.headers.getHeader("Transfer-Encoding").value.contains("chunked")) {
					thisClone.headers.addHeader("Content-Length", thisClone.body.getBody().data.length + ""); // TODO: chunked is incredibly broken
				}
				if (!thisClone.headers.hasHeader("Content-Type")) {
					thisClone.headers.addHeader("Content-Type", thisClone.body.getBody().type);
				}
			}else {
				thisClone.headers.updateHeader("Content-Length", "0");
			}
			finalc = JavaWebServer.pluginBus.processResponse(thisClone, request, ce, finalc);
			ByteArrayOutputStream ser = new ByteArrayOutputStream();
			ser.write((thisClone.httpVersion + " " + thisClone.statusCode + " " + thisClone.reasonPhrase + crlf).getBytes());
			if (finalc != null) {
				if (ce == ContentEncoding.gzip || ce == ContentEncoding.xgzip) {
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					GZIPOutputStream gout = new GZIPOutputStream(bout);
					gout.write(finalc, 0, finalc.length);
					gout.flush();
					gout.close();
					finalc = bout.toByteArray();
					if (thisClone.headers.hasHeader("Content-Length")) {
						thisClone.headers.getHeader("Content-Length").value = finalc.length + "";
					}
				}
				if (ce != ContentEncoding.identity) thisClone.headers.addHeader("Content-Encoding", ce.name);
			}
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
		return new String(serialize(ContentEncoding.identity));
	}
	
	public String toString(ContentEncoding ce) {
		return new String(serialize(ce));
	}
	
	public String toString2(ContentEncoding ce) {
		return new String(serialize(ce, false));
	}
	
	public void write(DataOutputStream out, ContentEncoding ce) throws IOException {
		out.write(serialize(ce));
		out.flush();
		if (headers.hasHeader("Connection")) {
			String c = headers.getHeader("Connection").value;
			if (c.equals("Close")) {
				out.close();
			}
		}
	}
	
	public static ResponsePacket read(DataInputStream in) throws IOException {
		ResponsePacket incomingResponse = new ResponsePacket();
		String statusLine = "";
		try {
			statusLine = in.readLine().trim();
		}catch (NullPointerException e) {
			return null;
		}
		int b = statusLine.indexOf(" ");
		incomingResponse.httpVersion = statusLine.substring(0, b);
		incomingResponse.statusCode = Integer.parseInt(statusLine.substring(b + 1, b = statusLine.indexOf(" ", b + 1)));
		incomingResponse.reasonPhrase = statusLine.substring(b + 1);
		Headers headers = incomingResponse.headers;
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
					try {
						length = Integer.parseInt(len, 16);
					}catch (NumberFormatException e) {
						continue;
					}
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
			headers.addHeader("Content-Length", bbody.length + "");
		}else if (hcl) {
			bbody = new byte[Integer.parseInt(headers.getHeader("Content-Length").value)];
			in.readFully(bbody);
		}
		MessageBody body = new MessageBody(incomingResponse, new Resource(bbody, headers.hasHeader("Content-Type") ? headers.getHeader("Content-Type").value : "application/octet-stream"));
		incomingResponse.body = body;
		return incomingResponse;
	}
}
