package org.avuna.httpd.util.unio;

import java.io.IOException;
import java.io.OutputStream;

public class BufferOutputStream extends OutputStream {
	private final Buffer buf;
	
	public BufferOutputStream(Buffer buf) {
		this.buf = buf;
	}
	
	@Override
	public void write(int b) throws IOException {
		buf.append(new byte[] { (byte) b }, 0, 1);
	}
	
	@Override
	public void write(byte[] b) throws IOException {
		buf.append(b, 0, b.length);
	}
	
	@Override
	public void write(byte[] b, int offset, int length) throws IOException {
		buf.append(b, offset, length);
	}
	
}
