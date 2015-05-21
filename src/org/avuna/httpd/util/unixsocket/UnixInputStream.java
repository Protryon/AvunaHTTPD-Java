package org.avuna.httpd.util.unixsocket;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import org.avuna.httpd.util.CLib;

public class UnixInputStream extends InputStream {
	private int sockfd = -1;
	
	public UnixInputStream(int sockfd) {
		this.sockfd = sockfd;
	}
	
	public int available() {
		int status = CLib.available(sockfd);
		if (status < 0) return 0;
		return status;
	}
	
	@Override
	public int read() throws IOException {
		byte[] sa = CLib.read(sockfd, 1);
		if (sa.length == 0) {
			int i = CLib.errno();
			if (i == 104) {
				throw new SocketException("Connection reset by peer!");
			}else throw new CException(i, "read failed");
		}
		return sa[0] & 0xff;
	}
	
	public int read(byte[] array) throws IOException {
		byte[] buf = CLib.read(sockfd, array.length);
		if (buf.length == 0) {
			int i = CLib.errno();
			if (i == 104) {
				throw new SocketException("Connection reset by peer!");
			}else throw new CException(i, "read failed");
		}
		System.arraycopy(buf, 0, array, 0, buf.length);
		return buf.length;
	}
	
	public int read(byte[] array, int off, int len) throws IOException {
		if (off + len > array.length) throw new ArrayIndexOutOfBoundsException("off + len MUST NOT be >= array.length");
		byte[] buf = CLib.read(sockfd, len);
		if (buf.length == 0) { // not 100% accurate, but what else?
			int i = CLib.errno();
			if (i == 104) {
				throw new SocketException("Connection reset by peer!");
			}else throw new CException(i, "read failed");
		}
		System.arraycopy(buf, 0, array, off, buf.length);
		return buf.length;
	}
	
}
