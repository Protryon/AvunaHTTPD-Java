/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.util.unio;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import org.avuna.httpd.util.CException;
import org.avuna.httpd.util.CLib;

public class UNIOOutputStream extends OutputStream {
	private UNIOSocket socket = null;
	
	public UNIOOutputStream(UNIOSocket socket) {
		this.socket = socket;
	}
	
	@Override
	public void write(int b) throws IOException {
		byte[] sa = new byte[] { (byte) b };
		int i = socket.session == 0L ? CLib.write(socket.sockfd, sa) : GNUTLS.write(socket.session, sa);
		if (i < 0) {
			i = CLib.errno();
			if (i == 104) {
				throw new SocketException("Connection reset by peer");
			}else if (i == 32) {
				throw new SocketException("Broken Pipe");
			}else throw new CException(i, "End of Stream");
		}
		socket.lr = System.currentTimeMillis();
	}
	
	public int cwrite(byte[] buf) throws IOException {
		return cwrite(buf, 0, buf.length);
	}
	
	public void write(byte[] buf) throws IOException {
		this.cwrite(buf, 0, buf.length);
	}
	
	public int cwrite(byte[] buf, int off, int len) throws IOException {
		if (len == 0) return 0;
		byte[] buf2 = new byte[len];
		System.arraycopy(buf, off, buf2, 0, len);
		int i = socket.session == 0L ? CLib.write(socket.sockfd, buf2) : GNUTLS.write(socket.session, buf2);
		if (i < 0) {
			i = socket.session == 0L ? CLib.errno() : i;
			if (i == 11 | i == -28) { // EAGAIN/GNUTLS_EAGAIN
				return 0;
			}else if (i == 104) {
				throw new SocketException("Connection reset by peer");
			}else if (i == 32) {
				throw new SocketException("Broken Pipe");
			}else throw new CException(i, "End of Stream");
		}
		socket.lr = System.currentTimeMillis();
		return i;
	}
	
	public void write(byte[] buf, int off, int len) throws IOException {
		this.cwrite(buf, off, len);
	}
	
	public void flush() {
	
	}
}
