package org.avuna.httpd.mail.smtp;

import java.io.IOException;
import org.avuna.httpd.util.unio.PacketReceiver;
import org.avuna.httpd.util.unio.UNIOSocket;

public class SMTPPacketReceiver extends PacketReceiver {
	
	private SMTPWork work = null;
	
	protected void setWork(SMTPWork w) {
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
			work.flushPacket(buf);
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
	
}
