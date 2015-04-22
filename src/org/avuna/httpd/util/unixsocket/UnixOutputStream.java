package org.avuna.httpd.util.unixsocket;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import org.avuna.httpd.CLib;
import org.avuna.httpd.CLib.bap;

public class UnixOutputStream extends OutputStream {
	private int sockfd = -1;
	
	public UnixOutputStream(int sockfd) {
		this.sockfd = sockfd;
	}
	
	@Override
	public void write(int b) throws IOException {
		bap bap = new bap(1);
		bap.array[0] = (byte)b;
		int i = CLib.INSTANCE.write(sockfd, bap, 1);
		if (i < 0) {
			throw new EOFException("End of Stream!");
		}
	}
	
	public void write(byte[] buf) throws IOException {
		bap bap = new bap(buf.length);
		System.arraycopy(buf, 0, bap.array, 0, buf.length);
		int i = CLib.INSTANCE.write(sockfd, bap, buf.length);
		if (i < 0) {
			throw new EOFException("End of Stream!");
		}
	}
	
	public void write(byte[] buf, int off, int len) throws IOException {
		bap bap = new bap(buf.length);
		System.arraycopy(buf, 0, bap.array, off, len);
		int i = CLib.INSTANCE.write(sockfd, bap, len);
		if (i < 0) {
			throw new EOFException("End of Stream!");
		}
	}
	
	public void flush() {
		// CLib.INSTANCE.fflush(sockfd);
	}
}
