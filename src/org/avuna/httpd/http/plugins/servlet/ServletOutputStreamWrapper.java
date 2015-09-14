package org.avuna.httpd.http.plugins.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import org.avuna.httpd.http.networking.ResponsePacket;

public class ServletOutputStreamWrapper extends ServletOutputStream {
	private final ResponsePacket response;
	
	protected ServletOutputStreamWrapper(ResponsePacket response) {
		this.response = response;
	}
	
	@Override
	public boolean isReady() {
		return true;
	}
	
	private WriteListener wl = null;
	private boolean lock = false;
	
	@Override
	public void setWriteListener(WriteListener arg0) {
		try {
			this.wl = arg0;
			lock = true;
			arg0.onWritePossible();
			lock = false;
		}catch (IOException e) {
			response.request.host.logger.logError(e);
		}
	}
	
	private final ByteArrayOutputStream bout = new ByteArrayOutputStream();
	
	@Override
	public void write(int b) throws IOException {
		bout.write(b);
		if (!lock) try {
			wl.onWritePossible();
		}catch (IOException e) {
			response.request.host.logger.logError(e);
		}
	}
	
	@Override
	public void write(byte[] b) throws IOException {
		bout.write(b);
		if (!lock) try {
			wl.onWritePossible();
		}catch (IOException e) {
			response.request.host.logger.logError(e);
		}
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		bout.write(b, off, len);
		if (!lock) try {
			wl.onWritePossible();
		}catch (IOException e) {
			response.request.host.logger.logError(e);
		}
	}
	
	public void flush() {
		response.body.data = bout.toByteArray();
	}
	
}
