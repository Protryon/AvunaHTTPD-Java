package org.avuna.httpd.util.unixsocket;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import org.avuna.httpd.util.CLib;

public class UnixOutputStream extends OutputStream {
	private int sockfd = -1;
	
	public UnixOutputStream(int sockfd) {
		this.sockfd = sockfd;
	}
	
	@Override
	public void write(int b) throws IOException {
		byte[] sa = new byte[]{(byte)b};
		int i = CLib.write(sockfd, sa);
		if (i < 0) {
			i = CLib.errno();
			if (i == 104) {
				throw new SocketException("Connection reset by peer");
			}else if (i == 32) {
				throw new SocketException("Broken Pipe");
			}else throw new CException(i, "End of Stream");
		}
	}
	
	public void write(byte[] buf) throws IOException {
		if (buf.length == 0) return;
		int i = CLib.write(sockfd, buf);
		if (i < 0) {
			i = CLib.errno();
			if (i == 104) {
				throw new SocketException("Connection reset by peer");
			}else if (i == 32) {
				throw new SocketException("Broken Pipe");
			}else throw new CException(i, "End of Stream");
		}
	}
	
	public void write(byte[] buf, int off, int len) throws IOException {
		if (len == 0) return;
		byte[] buf2 = new byte[len];
		System.arraycopy(buf, off, buf2, 0, len);
		int i = CLib.write(sockfd, buf2);
		if (i < 0) {
			i = CLib.errno();
			if (i == 104) {
				throw new SocketException("Connection reset by peer");
			}else if (i == 32) {
				throw new SocketException("Broken Pipe");
			}else throw new CException(CLib.errno(), "End of Stream");
		}
	}
	
	public void flush() {
		// CLib.INSTANCE.fflush(sockfd);
	}
}
