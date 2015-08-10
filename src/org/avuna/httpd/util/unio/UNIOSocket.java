/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.util.unio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import org.avuna.httpd.util.CException;
import org.avuna.httpd.util.CLib;

// you cannot make outgoing connections with this interface(it wouldn't provide any benefit over java TCP)

public class UNIOSocket extends Socket {
	protected int sockfd = -1;
	private String ip = "";
	private int port = -1;
	private boolean closed = false;
	private UNIOOutputStream out = null;
	private UNIOInputStream in = null;
	protected Buffer buf;
	private PacketReceiver callback;
	
	protected UNIOSocket(String ip, int port, int sockfd, PacketReceiver callback) {
		this.sockfd = sockfd;
		this.ip = ip;
		this.port = port;
		buf = new Buffer(1024, callback, this);
		out = new UNIOOutputStream(sockfd);
		in = new UNIOInputStream(sockfd);
		this.callback = callback;
	}
	
	public void setTcpNoDelay(boolean b) {
		
	}
	
	protected void read() throws IOException {
		byte[] b = new byte[in.available()];
		int i = 0;
		do {
			i += in.read(b, i, b.length - i);
		}while (i < b.length);
		buf.append(b);
	}
	
	public void setSoTimeout(int timeout) {
		// TODO: impl
	}
	
	public InetAddress getInetAddress() {
		try {
			InetAddress ia = InetAddress.getByName(ip);
			return ia == null ? InetAddress.getByName("0.0.0.0") : ia;
		}catch (UnknownHostException e) {
			return null;
		}
	}
	
	public int getPort() {
		return port;
	}
	
	public UNIOInputStream getInputStream() throws IOException {
		return in;
	}
	
	public UNIOOutputStream getOutputStream() throws IOException {
		return out;
	}
	
	public boolean isClosed() {
		return closed;
	}
	
	public void close() throws IOException {
		closed = true;
		int s = CLib.close(sockfd);
		if (s < 0) throw new CException(CLib.errno(), "socket failed close");
		callback.closed(this);
	}
	
	public PacketReceiver getCallback() {
		return callback;
	}
}
