package org.avuna.httpd.util.unio;

import java.io.IOException;
import java.net.ServerSocket;
import org.avuna.httpd.util.CException;
import org.avuna.httpd.util.CLib;

public class UNIOServerSocket extends ServerSocket {
	private int sockfd = 0;
	private boolean bound = false;
	private int backlog = 50;
	private boolean closed = false;
	private String ip;
	private int port;
	private PacketReceiverFactory factory;
	
	public boolean isClosed() {
		return closed;
	}
	
	public UNIOServerSocket(String ip, int port, PacketReceiverFactory factory, int backlog) throws IOException {
		this.backlog = backlog;
		this.ip = ip;
		this.port = port;
		this.factory = factory;
	}
	
	public UNIOServerSocket(String ip, int port, PacketReceiverFactory factory) throws IOException {
		this(ip, port, factory, 50);
	}
	
	public void bind() throws IOException {
		if (bound) throw new IOException("Already bound!");
		sockfd = CLib.socket(1, 1, 0);
		if (sockfd < 0) throw new CException(CLib.errno(), "socket failed native create");
		int bind = CLib.bindTCP(sockfd, ip, port);
		if (bind != 0) throw new CException(CLib.errno(), "socket failed bind");
		int listen = CLib.listen(sockfd, this.backlog);
		if (listen != 0) throw new CException(CLib.errno(), "socket failed listen");
		bound = true;
	}
	
	public UNIOSocket accept() throws IOException {
		if (!bound) bind();
		// Logger.log("accepting");
		String nsfd = CLib.accept(sockfd);
		// Logger.log(nsfd);
		int i = Integer.parseInt(nsfd.substring(0, nsfd.indexOf("/")));
		nsfd = nsfd.substring(nsfd.indexOf("/") + 1);
		CLib.noblock(i);
		UNIOSocket us = new UNIOSocket(nsfd, port, i, factory.newCallback());
		return us;
	}
	
	public void close() throws IOException {
		closed = true;
		int s = CLib.close(sockfd);
		if (s < 0) throw new CException(CLib.errno(), "socket failed close");
	}
}
