/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.networking;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import org.avuna.httpd.util.Stream;

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
			String hex = Stream.readLine(in);
			if (hex.length() == 0) {
				hex = Stream.readLine(in);
			}
			int l = Integer.parseInt(hex.trim(), 16);
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
			String hex = Stream.readLine(in);
			if (hex.length() == 0) {
				hex = Stream.readLine(in);
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
