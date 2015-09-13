/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.util.unixsocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import org.avuna.httpd.util.CException;
import org.avuna.httpd.util.CLib;

public class UnixSocket extends Socket {
	private int sockfd = -1;
	private String file = "";
	private boolean connected = false, servermade = false, closed = false;
	private UnixOutputStream out = null;
	private UnixInputStream in = null;
	
	protected UnixSocket(String file, int sockfd) {
		this.sockfd = sockfd;
		this.file = file;
		out = new UnixOutputStream(sockfd);
		in = new UnixInputStream(sockfd);
		connected = true;
		servermade = true;
	}
	
	public void setTcpNoDelay(boolean b) {
	
	}
	
	public UnixSocket(String file) {
		this.file = file;
	}
	
	public void setSoTimeout(int timeout) {
		// TODO: impl
	}
	
	public void connect() throws IOException {
		if (connected) throw new IOException("Already connected!");
		sockfd = CLib.socket(1, 1, 0);
		if (sockfd < 0) throw new CException(CLib.errno(), "socket failed native create");
		int c = CLib.connect(sockfd, file);
		if (c != 0) switch (CLib.errno()) {
			case 111:
				throw new SocketException("Connection Refused");
			case 110:
				throw new SocketException("Connection Timed Out");
			default:
				throw new CException(CLib.errno(), "socket failed connect");
		}
		out = new UnixOutputStream(sockfd);
		in = new UnixInputStream(sockfd);
		connected = true;
	}
	
	public InetAddress getInetAddress() {
		try {
			return InetAddress.getLocalHost();
		}catch (UnknownHostException e) {
			return null;
		}
	}
	
	public InetAddress getLocalAddress() {
		try {
			return InetAddress.getLocalHost();
		}catch (UnknownHostException e) {
			return null;
		}
	}
	
	public int getPort() {
		return -1;
	}
	
	public int getLocalPort() {
		return -1;
	}
	
	public UnixInputStream getInputStream() throws IOException {
		if (!connected && !servermade) connect();
		return in;
	}
	
	public UnixOutputStream getOutputStream() throws IOException {
		if (!connected && !servermade) connect();
		return out;
	}
	
	public boolean isClosed() {
		return closed;
	}
	
	public void close() throws IOException {
		closed = true;
		int s = CLib.close(sockfd);
		if (s < 0) throw new CException(CLib.errno(), "socket failed close");
	}
}
