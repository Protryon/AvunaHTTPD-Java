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
	private long cert = 0L;
	
	public long getCertificate() {
		return cert;
	}
	
	public boolean isClosed() {
		return closed;
	}
	
	// TODO: throws IO exception because it calls the super constructor(but we don't need/want it to.)
	public UNIOServerSocket(String ip, int port, PacketReceiverFactory factory, int backlog) throws IOException {
		this.backlog = backlog;
		this.ip = ip;
		this.port = port;
		this.factory = factory;
	}
	
	public UNIOServerSocket(String ip, int port, PacketReceiverFactory factory) throws IOException {
		this(ip, port, factory, 50);
	}
	
	public UNIOServerSocket(String ip, int port, PacketReceiverFactory factory, int backlog, String ca, String cert, String key) throws IOException {
		this(ip, port, factory, backlog);
		this.cert = GNUTLS.loadcert(ca, cert, key);
		if (this.cert <= 0L) {
			throw new CException((int) this.cert, "Failed to load SSL certificate!");
		}
	}
	
	public void bind() throws IOException {
		if (bound) throw new IOException("Already bound!");
		sockfd = CLib.socket(2, 1, 0);
		if (sockfd < 0) {
			this.close();
			throw new CException(CLib.errno(), "socket failed native create");
		}
		int bind = CLib.bindTCP(sockfd, ip, port);
		if (bind != 0) {
			this.close();
			throw new CException(CLib.errno(), "socket failed bind");
		}
		int listen = CLib.listen(sockfd, this.backlog);
		if (listen != 0) {
			this.close();
			throw new CException(CLib.errno(), "socket failed listen");
		}
		bound = true;
	}
	
	public UNIOSocket accept() throws IOException {
		if (!bound) bind();
		// Logger.log("accepting");
		long session = 0L;
		if (cert > 0L) session = GNUTLS.preaccept(cert);
		String nsfd = CLib.acceptTCP(sockfd);
		// Logger.log(nsfd);
		int i = Integer.parseInt(nsfd.substring(0, nsfd.indexOf("/")));
		if (i == -1) {
			this.close();
			throw new CException(CLib.errno(), "Server closed!");
		}
		if (cert > 0L) {
			int e = GNUTLS.postaccept(cert, session, i);
			if (e < 0) {
				throw new CException(e, "Failed TCP Handshake!");
			}
		}
		nsfd = nsfd.substring(nsfd.indexOf("/") + 1);
		UNIOSocket us = new UNIOSocket(nsfd, port, i, factory == null ? null : factory.newCallback(this), cert > 0L ? session : 0L);
		return us;
	}
	
	public void close() throws IOException {
		closed = true;
		int s = CLib.close(sockfd);
		if (s < 0) throw new CException(CLib.errno(), "socket failed close");
	}
}
