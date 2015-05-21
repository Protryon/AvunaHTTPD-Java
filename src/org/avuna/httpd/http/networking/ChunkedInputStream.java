package org.avuna.httpd.http.networking;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class ChunkedInputStream extends InputStream {
	private ByteArrayInputStreamExtended buf = new ByteArrayInputStreamExtended();
	private InputStream proxy = null;
	private final DataInputStream in;
	
	public ChunkedInputStream(DataInputStream in, boolean gzip) throws IOException {
		this.in = in;
		if (gzip) {
			proxy = new GZIPInputStream(buf);
		}else {
			proxy = buf;
		}
	}
	
	private boolean ended = false;
	
	public int read() throws IOException {
		if (ended) return -1;
		if (proxy.available() == 0) {
			String hex = in.readLine();
			if (hex.length() == 0) {
				hex = in.readLine();
			}
			int l = Integer.parseInt(hex, 16);
			if (l == 0) {
				ended = true;
				return -1;
			}
			byte[] buf = new byte[l];
			in.readFully(buf);
			this.buf.restart(buf);
		}
		return buf.read();
	}
	
	public int blockAvailable(boolean allowRefresh) throws IOException {
		if (ended) return 0;
		if (allowRefresh && proxy.available() == 0) {
			String hex = in.readLine();
			if (hex.length() == 0) {
				hex = in.readLine();
			}
			int l = Integer.parseInt(hex, 16);
			if (l == 0) {
				ended = true;
				return -1;
			}
			byte[] buf = new byte[l];
			in.readFully(buf);
			this.buf.restart(buf);
		}
		return proxy.available();
	}
	
	public int available() throws IOException {
		if (ended) return 0;
		return in.available() + proxy.available();
	}
	
	public boolean isEnded() {
		return ended;
	}
}
