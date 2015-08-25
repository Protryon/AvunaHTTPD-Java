/*	Avuna HTTPD - General Server Applications
    Copyright (C) 2015 Maxwell Bruce

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.avuna.httpd.http.networking;

import java.io.IOException;
import org.avuna.httpd.AvunaHTTPD;
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
	
	@Override
	public void fail(Exception e) {
		if (work != null) work.host.logger.logError(e);
		else AvunaHTTPD.logger.logError(e);
	}
	
}
