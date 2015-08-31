/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.networking;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.zip.GZIPOutputStream;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.util.unio.BufferOutputStream;

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
	private boolean writing = false;
	
	public void writeHeaders() throws IOException {
		if (!flushed && !writing) {
			StringBuilder ser = new StringBuilder();
			ser.append((toSend.httpVersion + " " + toSend.statusCode + " " + toSend.reasonPhrase + AvunaHTTPD.crlf));
			HashMap<String, String[]> hdrs = toSend.headers.getHeaders();
			for (String key : hdrs.keySet()) {
				for (String val : hdrs.get(key)) {
					ser.append((key + ": " + val + AvunaHTTPD.crlf));
				}
			}
			ser.append(AvunaHTTPD.crlf);
			writing = true;
			super.write(ser.toString().getBytes());
			super.flush();
			writing = false;
			flushed = true;
		}
	}
	
	public void write(byte[] b, int off, int len) throws IOException {
		if (writing) {
			super.write(b, off, len);
		}else {
			if (!flushed) {
				writeHeaders();
			}else {
				cache.write(b, off, len);
			}
		}
	}
	
	public void flush() throws IOException {
		byte[] cache = this.cache.toByteArray();
		this.cache.reset();
		if (cache.length == 0) return;
		if (gzip) {
			gzips.write(cache);
			gzips.flush();
			cache = gzipb.toByteArray();
			gzipb.reset();
		}
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(0, cache.length);
		byte[] bas = new byte[4];
		bb.position(0);
		bb.get(bas);
		String hex = AvunaHTTPD.fileManager.bytesToHex(bas);
		writing = true;
		super.write((hex + AvunaHTTPD.crlf).getBytes());
		super.write(cache);
		super.write(AvunaHTTPD.crlf.getBytes());
		if (super.out instanceof BufferOutputStream) ((BufferOutputStream) super.out).getBuffer().getSocket().flush(-1L);
		else super.flush();
		writing = false;
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
			writing = true;
			super.write((AvunaHTTPD.fileManager.bytesToHex(bas) + AvunaHTTPD.crlf).getBytes());
			super.write(cache);
			super.write((AvunaHTTPD.crlf + "0" + AvunaHTTPD.crlf).getBytes());
			if (super.out instanceof BufferOutputStream) ((BufferOutputStream) super.out).getBuffer().getSocket().flush(-1L);
			else super.flush();
			writing = false;
		}else {
			writing = true;
			super.write(("0" + AvunaHTTPD.crlf).getBytes());
			super.flush();
			writing = false;
		}
	}
	
	public void close() throws IOException {
		finish();
		super.close();
	}
	
}
