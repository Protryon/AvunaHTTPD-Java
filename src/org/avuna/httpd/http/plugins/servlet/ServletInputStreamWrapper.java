package org.avuna.httpd.http.plugins.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import org.avuna.httpd.http.networking.RequestPacket;

public class ServletInputStreamWrapper extends ServletInputStream {
	private final RequestPacket request;
	
	protected ServletInputStreamWrapper(RequestPacket request) {
		this.request = request;
		this.bin = new ByteArrayInputStream(request.body.data);
	}
	
	private int i = 0;
	private ByteArrayInputStream bin;
	
	@Override
	public boolean isFinished() {
		return request.body.data.length >= i;
	}
	
	@Override
	public boolean isReady() {
		return !isFinished();
	}
	
	private ReadListener listener = null;
	
	@Override
	public void setReadListener(ReadListener arg0) {
		try {
			arg0.onDataAvailable();
		}catch (IOException e) {
			request.host.logger.logError(e);
		}
		listener = arg0;
	}
	
	@Override
	public int read() throws IOException {
		int b = bin.read();
		if (listener != null && isFinished()) {
			listener.onAllDataRead();
		}
		return b;
	}
	
	public int read(byte[] buf) throws IOException {
		int b = bin.read(buf);
		if (listener != null && isFinished()) {
			listener.onAllDataRead();
		}
		return b;
	}
	
	public int read(byte[] buf, int off, int len) throws IOException {
		int b = bin.read(buf, off, len);
		if (listener != null && isFinished()) {
			listener.onAllDataRead();
		}
		return b;
	}
	
}
