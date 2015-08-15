package org.avuna.httpd.ftp;

import java.io.IOException;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.util.unio.PacketReceiver;
import org.avuna.httpd.util.unio.UNIOSocket;

public class FTPPacketReceiver extends PacketReceiver {
	
	private FTPWork work = null;
	
	protected void setWork(FTPWork w) {
		this.work = w;
		synchronized (this) {
			this.notify();
		}
	}
	
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
			work.flushPacket(buf); // >= 0 if we have a body
		}catch (IOException e) {
			work.host.logger.logError(e);
		}
	}
	
	@Override
	public int nextDelimType(UNIOSocket sock) {
		return 0;
	}
	
	public byte[] nextDelim(UNIOSocket sock) {
		return "\r\n".getBytes();
	}
	
	@Override
	public void closed(UNIOSocket sock) {
		if (work != null) try {
			work.close();
		}catch (IOException e) {
			work.host.logger.logError(e);
		}
	}
	
	@Override
	public void fail(Exception e) {
		if (work != null) work.host.logger.logError(e);
		else AvunaHTTPD.logger.logError(e);
	}
	
}
