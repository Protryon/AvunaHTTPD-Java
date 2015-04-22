package org.avuna.httpd.util.unixsocket;

import java.io.IOException;
import java.io.InputStream;
import org.avuna.httpd.CLib;
import org.avuna.httpd.CLib.bap;
import com.sun.jna.ptr.IntByReference;

public class UnixInputStream extends InputStream {
	private int sockfd = -1;
	
	public UnixInputStream(int sockfd) {
		this.sockfd = sockfd;
	}
	
	public int available() {
		IntByReference ava = new IntByReference(0);
		int status = CLib.INSTANCE.ioctl(sockfd, 0x541B, ava);
		if (status < 0) return 0;
		return ava.getValue();
	}
	
	@Override
	public int read() throws IOException {
		bap bap = new bap(1);
		CLib.INSTANCE.read(sockfd, bap, 1);
		return bap.array[0];
	}
	
	public int read(byte[] array) {
		bap bap = new bap(array.length);
		int i = CLib.INSTANCE.read(sockfd, bap, array.length);
		System.arraycopy(bap.array, 0, array, 0, array.length);
		return i;
	}
	
	public int read(byte[] array, int off, int len) {
		if (off + len >= array.length) throw new ArrayIndexOutOfBoundsException("off + len MUST NOT be >= array.length");
		bap bap = new bap(array.length);
		int i = CLib.INSTANCE.read(sockfd, bap, array.length);
		System.arraycopy(bap.array, 0, array, off, len);
		return i;
	}
	
}
