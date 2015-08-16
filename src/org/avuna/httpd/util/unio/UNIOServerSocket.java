package org.avuna.httpd.util.unio;

import java.io.IOException;
import java.net.BindException;
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
	private Certificate cert = null;
	private SNICallback sni = null;
	
	public Certificate getCertificate() {
		return cert;
	}
	
	public SNICallback getSNICallback() {
		return sni;
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
	
	public UNIOServerSocket(String ip, int port, PacketReceiverFactory factory, int backlog, Certificate cert, SNICallback sni) throws IOException {
		this(ip, port, factory, backlog);
		this.cert = cert;
		this.sni = sni;
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
			int e = CLib.errno();
			if (e == 98) {
				throw new BindException();
			}else {
				throw new CException(e, "socket failed bind");
			}
		}
		int listen = CLib.listen(sockfd, this.backlog);
		if (listen != 0) {
			this.close();
			throw new CException(CLib.errno(), "socket failed listen");
		}
		bound = true;
	}
	
	public UNIOSocket accept() throws IOException {
		if (closed) throw new IOException("Server closed!");
		if (!bound) bind();
		long session = 0L;
		long cert = this.cert == null ? 0L : this.cert.getRawCertificate();
		if (cert > 0L) session = GNUTLS.preaccept(cert);
		String nsfd = CLib.acceptTCP(sockfd);
		int i = Integer.parseInt(nsfd.substring(0, nsfd.indexOf("/")));
		if (i == -1) {
			int e = CLib.errno();
			this.close();
			if (e == 24) throw new IOException("Too many open files!");
			else throw new CException(e, "Server closed!");
		}
		if (cert > 0L) {
			int e = GNUTLS.postaccept(cert, session, i, sni);
			if (e < 0) {
				throw new CException(e, "Failed TLS Handshake!");
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
