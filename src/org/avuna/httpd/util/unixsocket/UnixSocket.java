package org.avuna.httpd.util.unixsocket;

import java.io.IOException;
import org.avuna.httpd.CLib;
import com.sun.jna.Native;

public class UnixSocket {
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
	
	public UnixSocket(String file) {
		this.file = file;
	}
	
	public void connect() throws IOException {
		if (connected) throw new IOException("Already connected!");
		sockfd = CLib.INSTANCE.socket(1, 1, 0);
		if (sockfd < 0) throw new CException(Native.getLastError(), "socket failed native create");
		CLib.sockaddr_un sockaddr = new CLib.sockaddr_un();
		byte[] fd = file.getBytes();
		sockaddr.sunfamily = 1;
		sockaddr.sunpath = fd;
		int c = CLib.INSTANCE.connect(sockfd, sockaddr, fd.length + 2);
		if (c != 0) throw new CException(Native.getLastError(), "socket failed connect");
		out = new UnixOutputStream(sockfd);
		in = new UnixInputStream(sockfd);
		connected = true;
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
		int s = CLib.INSTANCE.close(sockfd);
		if (s < 0) throw new CException(Native.getLastError(), "socket failed close");
	}
}
