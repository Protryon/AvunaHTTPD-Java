package org.avuna.httpd.http.networking;

import java.io.IOException;
import org.avuna.httpd.util.unio.PacketReceiver;
import org.avuna.httpd.util.unio.UNIOSocket;

public class HTTPPacketReceiver extends PacketReceiver {
	
	private Work work = null;
	
	protected void setWork(Work w) {
		this.work = w;
		synchronized (this) {
			this.notify();
		}
	}
	
	private boolean expectingBody = false;
	private int bodyLength = -1;
	
	@Override
	public void readPacket(UNIOSocket sock, byte[] buf) {
		if (work == null) {
			synchronized (this) {
				try {
					this.wait();
				}catch (InterruptedException e) {}
			}
		}
		try {
			int i = work.flushPacket(buf); // >= 0 if we have a body
			if (i >= 0) {
				expectingBody = true;
				bodyLength = i;
			}
		}catch (IOException e) {
			work.host.logger.logError(e);
		}
	}
	
	@Override
	public int nextDelimType(UNIOSocket sock) {
		return expectingBody ? 1 : 0;
	}
	
	public byte[] nextDelim(UNIOSocket sock) {
		return "\r\n\r\n".getBytes();
	}
	
	public int nextLength(UNIOSocket sock) {
		int i = bodyLength;
		bodyLength = -1;
		expectingBody = false;
		return i;
	}
	
	@Override
	public void closed(UNIOSocket sock) {
		if (work != null) work.close();
	}
	
}
