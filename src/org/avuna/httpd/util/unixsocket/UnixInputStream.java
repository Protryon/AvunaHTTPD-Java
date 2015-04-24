package org.avuna.httpd.util.unixsocket;

import java.io.IOException;
import java.io.InputStream;
import org.avuna.httpd.util.CLib;
import org.avuna.httpd.util.CLib.bap;
import com.sun.jna.Native;
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
		int i = CLib.INSTANCE.read(sockfd, bap, 1);
		if (i < 0) {
			throw new CException(Native.getLastError(), "read failed");
		}
		return bap.array[0] & 0xff;
	}
	
	public int read(byte[] array) throws IOException {
		bap bap = new bap(array.length);
		int i = CLib.INSTANCE.read(sockfd, bap, array.length);
		if (i < 0) {
			throw new CException(Native.getLastError(), "read failed");
		}
		System.arraycopy(bap.array, 0, array, 0, array.length);
		return i;
	}
	
	public int read(byte[] array, int off, int len) throws IOException {
		if (off + len > array.length) throw new ArrayIndexOutOfBoundsException("off + len MUST NOT be >= array.length");
		bap bap = new bap(array.length);
		int i = CLib.INSTANCE.read(sockfd, bap, array.length);
		if (i < 0) {
			throw new CException(Native.getLastError(), "read failed");
		}
		System.arraycopy(bap.array, 0, array, off, len);
		return i;
	}
	
}
