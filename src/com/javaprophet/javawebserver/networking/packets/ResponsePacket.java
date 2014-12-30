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
import com.javaprophet.javawebserver.networking.Packet;

public class ResponsePacket extends Packet {
	public int statusCode = 200;
	public String reasonPhrase = "";
	public boolean isHead = false;
	
	public byte[] serialize(ContentEncoding ce) {
		return serialize(ce, true);
	}
	
	public byte[] serialize(ContentEncoding ce, boolean data) {
		try {
			Headers hc = headers.clone();
			ByteArrayOutputStream ser = new ByteArrayOutputStream();
			ser.write((httpVersion + " " + statusCode + " " + reasonPhrase + crlf).getBytes());
			if (body != null) {
				if (hc.hasHeader("Content-Length") && !isHead) {
					hc.getHeader("Content-Length").value = body.getBody().length + "";
				}else if (!hc.hasHeader("Transfer-Encoding") || !hc.getHeader("Transfer-Encoding").value.contains("chunked")) {
					hc.addHeader("Content-Length", body.getBody().length + ""); // TODO: chunked is incredibly broken
				}
				if (!hc.hasHeader("Content-Type")) {
					hc.addHeader("Content-Type", body.getContentType());
				}
			}
			byte[] finalc = body.getBody();
			finalc = JavaWebServer.pluginBus.processResponse(hc, ce, data, finalc);
			if (ce == ContentEncoding.gzip || ce == ContentEncoding.xgzip) {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				GZIPOutputStream gout = new GZIPOutputStream(bout);
				gout.write(finalc, 0, finalc.length);
				gout.flush();
				gout.close();
				finalc = bout.toByteArray();
				if (hc.hasHeader("Content-Length")) {
					hc.getHeader("Content-Length").value = finalc.length + "";
				}
			}
			if (ce != ContentEncoding.identity) hc.addHeader("Content-Encoding", ce.name);
			for (Header header : hc.getHeaders()) {
				ser.write((header.toLine() + crlf).getBytes());
			}
			ser.write(crlf.getBytes());
			if (data) ser.write(finalc);
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
		MessageBody body = new MessageBody(incomingResponse, bbody);
		incomingResponse.body = body;
		return incomingResponse;
	}
}
