/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.util.unio;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import org.avuna.httpd.util.CException;
import org.avuna.httpd.util.CLib;

public class UNIOOutputStream extends OutputStream {
	private int sockfd = -1;
	private long session = 0L;
	
	public UNIOOutputStream(int sockfd) {
		this.sockfd = sockfd;
	}
	
	// ssl
	public UNIOOutputStream(int sockfd, long session) {
		this(sockfd);
		this.session = session;
	}
	
	@Override
	public void write(int b) throws IOException {
		byte[] sa = new byte[] { (byte) b };
		int i = session == 0L ? CLib.write(sockfd, sa) : GNUTLS.write(session, sa);
		if (i < 0) {
			i = CLib.errno();
			if (i == 104) {
				throw new SocketException("Connection reset by peer");
			}else if (i == 32) {
				throw new SocketException("Broken Pipe");
			}else throw new CException(i, "End of Stream");
		}
	}
	
	public int cwrite(byte[] buf) throws IOException {
		if (buf.length == 0) return 0;
		int i = session == 0L ? CLib.write(sockfd, buf) : GNUTLS.write(session, buf);
		if (i < 0) {
			i = CLib.errno();
			if (i == 104) {
				throw new SocketException("Connection reset by peer");
			}else if (i == 32) {
				throw new SocketException("Broken Pipe");
			}else throw new CException(i, "End of Stream");
		}
		return i;
	}
	
	public void write(byte[] buf) throws IOException {
		this.cwrite(buf, 0, buf.length);
	}
	
	public int cwrite(byte[] buf, int off, int len) throws IOException {
		if (len == 0) return 0;
		byte[] buf2 = new byte[len];
		System.arraycopy(buf, off, buf2, 0, len);
		int i = session == 0L ? CLib.write(sockfd, buf2) : GNUTLS.write(session, buf2);
		if (i < 0) {
			i = CLib.errno();
			if (i == 104) {
				throw new SocketException("Connection reset by peer");
			}else if (i == 32) {
				throw new SocketException("Broken Pipe");
			}else throw new CException(CLib.errno(), "End of Stream");
		}
		return i;
	}
	
	public void write(byte[] buf, int off, int len) throws IOException {
		this.cwrite(buf, off, len);
	}
	
	public void flush() {
		// CLib.INSTANCE.fflush(sockfd);
	}
}
