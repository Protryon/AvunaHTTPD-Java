package com.javaprophet.javawebserver.plugins.javaloader;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPOutputStream;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;

public class ChunkedOutputStream extends DataOutputStream {
	private boolean gzip = false, flushed = false;
	private ResponsePacket toSend = null;
	private GZIPOutputStream gzips = null;
	
	public ChunkedOutputStream(OutputStream out, ResponsePacket toSend, boolean gzip) throws IOException {
		super(out);
		this.toSend = toSend;
		this.gzip = gzip;
		if (gzip) {
			gzips = new GZIPOutputStream(gzipb);
		}
	}
	
	private ByteArrayOutputStream cache = new ByteArrayOutputStream(), gzipb = new ByteArrayOutputStream();
	
	public void writeHeaders() throws IOException {
		if (!flushed) {
			flushed = true;
			StringBuilder ser = new StringBuilder();
			ser.append((toSend.httpVersion + " " + toSend.statusCode + " " + toSend.reasonPhrase + RequestPacket.crlf));
			HashMap<String, ArrayList<String>> hdrs = toSend.headers.getHeaders();
			for (String key : hdrs.keySet()) {
				for (String val : hdrs.get(key)) {
					ser.append((key + ": " + val + RequestPacket.crlf));
				}
			}
			ser.append(RequestPacket.crlf);
			super.write(ser.toString().getBytes());
			super.flush();
		}
	}
	
	public void write(byte[] b, int off, int len) throws IOException {
		if (!flushed) {
			writeHeaders();
		}else {
			cache.write(b, off, len);
		}
	}
	
	public void flush() throws IOException {
		byte[] cache = this.cache.toByteArray();
		this.cache.reset();
		if (gzip) {
			gzips.write(cache);
			cache = gzipb.toByteArray();
			gzipb.reset();
		}
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(0, cache.length);
		byte[] bas = new byte[4];
		bb.position(0);
		bb.get(bas);
		super.write((JavaWebServer.fileManager.bytesToHex(bas) + ResponsePacket.crlf).getBytes());
		super.write(cache);
		super.write(ResponsePacket.crlf.getBytes());
		super.flush();
	}
	
	public void finish() throws IOException {
		writeHeaders();
		if (cache.size() > 0) {
			flush();
		}
		if (gzip) {
			gzips.flush();
			gzips.close();
			byte[] cache = gzipb.toByteArray();
			gzipb.reset();
			ByteBuffer bb = ByteBuffer.allocate(4);
			bb.putInt(0, cache.length);
			byte[] bas = new byte[4];
			bb.position(0);
			bb.get(bas);
			super.write((JavaWebServer.fileManager.bytesToHex(bas) + ResponsePacket.crlf).getBytes());
			super.write(cache);
			super.write((ResponsePacket.crlf + "0" + ResponsePacket.crlf).getBytes());
			super.flush();
		}else {
			super.write(("0" + ResponsePacket.crlf).getBytes());
			super.flush();
		}
	}
	
	public void close() throws IOException {
		finish();
		super.close();
	}
	
}
