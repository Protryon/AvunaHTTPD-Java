package org.avuna.httpd.util.unixsocket;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import org.avuna.httpd.util.CLib;

public class UnixServerSocket extends ServerSocket {
	private int sockfd = 0;
	private boolean bound = false;
	private String file = "";
	private int backlog = 50;
	private boolean closed = false;
	
	public boolean isClosed() {
		return closed;
	}
	
	public UnixServerSocket(String file, int backlog) throws IOException {
		this.backlog = backlog;
		File f = new File(file);
		f.getParentFile().mkdirs();
		f.delete();
		this.file = f.getAbsolutePath();
	}
	
	public UnixServerSocket(String file) throws IOException {
		this(file, 50);
	}
	
	public void bind() throws IOException {
		if (bound) throw new IOException("Already bound!");
		sockfd = CLib.socket(1, 1, 0);
		if (sockfd < 0) throw new CException(CLib.errno(), "socket failed native create");
		int bind = CLib.bind(sockfd, file);
		if (bind != 0) throw new CException(CLib.errno(), "socket failed bind");
		int listen = CLib.listen(sockfd, 50);
		if (listen != 0) throw new CException(CLib.errno(), "socket failed listen");
		bound = true;
	}
	
	public UnixSocket accept() throws IOException {
		if (!bound) bind();
		String nsfd = CLib.accept(sockfd);
		int i = Integer.parseInt(nsfd.substring(0, nsfd.indexOf("/")));
		nsfd = nsfd.substring(nsfd.indexOf("/") + 1);
		UnixSocket us = new UnixSocket(file, i);
		return us;
	}
	
	public void close() throws IOException {
		closed = true;
		int s = CLib.close(sockfd);
		if (s < 0) throw new CException(CLib.errno(), "socket failed close");
	}
}
