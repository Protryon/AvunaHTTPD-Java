package org.avuna.httpd.util.unixsocket;

import java.io.IOException;
import org.avuna.httpd.CLib;

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
		if (sockfd < 0) throw new IOException("Socket failed to be created!");
		CLib.sockaddr_un sockaddr = new CLib.sockaddr_un();
		byte[] fd = file.getBytes();
		sockaddr.sunfamily = 1;
		sockaddr.sunpath = fd;
		int c = CLib.INSTANCE.connect(sockfd, sockaddr, fd.length + 2);
		if (c != 0) throw new IOException("Socket failed to connect!");
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
	
	public void close() throws IOException {
		closed = true;
		int s = CLib.INSTANCE.close(sockfd);
		if (s < 0) throw new IOException("Closing failed!");
	}
}
