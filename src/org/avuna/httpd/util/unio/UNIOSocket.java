/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.util.unio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
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
	private Buffer outBuf = new Buffer(1024, null, this);
	private BufferOutputStream ob = new BufferOutputStream(outBuf);
	private UNIOInputStream in = null;
	protected Buffer buf;
	private PacketReceiver callback;
	protected long session = 0L;// ssl
	private long msTimeout = 0L;
	protected long lr = System.currentTimeMillis();
	private boolean holdTimeout = false;
	protected boolean stlsi = false;
	protected boolean to = false;
	
	public boolean isSecure() {
		return session > 0L;
	}
	
	/** Performs a blocking flush, with a timeout, returns true if fully flushed, false if not. */
	public boolean flush(long timeout) throws IOException {
		if (closed) throw new SocketException("Socket Closed!");
		long start = System.currentTimeMillis() + timeout;
		if (timeout < 0) {
			start = -1;
		}else if (timeout == 0) {
			start = 0;
		}
		while (outBuf.available() > 0) { // could block, we should do something more like gnutls_handshake
			if (write() == 0) {
				try {
					Thread.sleep(1L); // prevent a full speed loop while waiting for kernel buffers to clear. 1 MS = more than enough
				}catch (InterruptedException e) {}
			}
			if ((start > 0 && System.currentTimeMillis() >= start) || start == 0) return outBuf.available() == 0;
		}
		return true;
	}
	
	public void starttls(Certificate cert, SNICallback sni) throws IOException {
		if (closed) throw new SocketException("Socket Closed!");
		if (cert == null || CLib.hasGNUTLS() != 1) return;
		long rcert = cert.getRawCertificate();
		stlsi = true;
		flush(100L); // wait for up to 100 ms
		this.session = GNUTLS.preaccept(rcert);
		if (this.session <= 0L) {
			stlsi = false;
			this.close();
			throw new IOException("Failed TCP Session create!");
		}
		int e = GNUTLS.postaccept(rcert, this.session, sockfd, sni);
		if (e < 0) {
			stlsi = false;
			this.close();
			throw new CException(e, "Failed TCP Handshake!");
		}
		stlsi = false;
		lr = System.currentTimeMillis();
	}
	
	public void setHoldTimeout(boolean holdTimeout) {
		if (!holdTimeout) {
			this.lr = System.currentTimeMillis(); // reset timeout
		}
		this.holdTimeout = holdTimeout;
	}
	
	public boolean getHoldTimeout() {
		return this.holdTimeout;
	}
	
	public void setFlushInterruptThread(Thread t) {
		outBuf.setFlushInterruptThread(t);
	}
	
	public void setTimeout(long t) {
		msTimeout = t;
	}
	
	public long getTimeout() {
		return msTimeout;
	}
	
	// ssl = session > 0
	protected UNIOSocket(String ip, int port, int sockfd, PacketReceiver callback, long session) {
		this.sockfd = sockfd;
		this.ip = ip;
		this.port = port;
		buf = new Buffer(1024, callback, this);
		out = new UNIOOutputStream(this);
		in = new UNIOInputStream(this);
		this.callback = callback;
		this.session = session;
	}
	
	/** Compatibility function, called automatically in C. */
	public void setTcpNoDelay(boolean b) {
	
	}
	
	protected int read() throws IOException {
		if (closed) throw new SocketException("Socket Closed!");
		byte[] b = new byte[in.available()];
		int i = 0;
		int li = 0;
		do {
			li = in.read(b, i, b.length - i);
			i += li;
		}while (li > 0 && i < b.length);
		if (i > 0) buf.append(b, 0, i);
		lr = System.currentTimeMillis();
		return i;
	}
	
	protected int write() throws IOException {
		if (closed) throw new SocketException("Socket Closed!");
		if (outBuf.available() < 1) return 0;
		byte[] b = new byte[4096];
		int i = 0;
		int wi = 0;
		int ti = 0;
		do {
			i = outBuf.read(b);
			if (i > 0) {
				wi = out.cwrite(b, 0, i);
				if (wi < i) {
					outBuf.unsafe_prepend(b, wi, i - wi);
				}
				ti += wi;
			}
		}while (i > 0 && wi >= i && wi > 0);
		lr = System.currentTimeMillis();
		return ti;
	}
	
	/** Compatibility function, this is NIO. */
	public void setSoTimeout(int timeout) {}
	
	public InetAddress getInetAddress() {
		try {
			InetAddress ia = InetAddress.getByName(ip);
			return ia == null ? InetAddress.getByName("0.0.0.0") : ia;
		}catch (UnknownHostException e) {
			return null;
		}
	}
	
	public InetAddress getLocalAddress() {
		try {
			return InetAddress.getByName(ip);
		}catch (UnknownHostException e) {
			return null;
		}
	}
	
	public int getPort() {
		return -1;
	}
	
	public int getLocalPort() {
		return port;
	}
	
	public InputStream getInputStream() throws IOException {
		return buf;
	}
	
	public OutputStream getOutputStream() throws IOException {
		return ob;
	}
	
	public boolean isClosed() {
		return closed;
	}
	
	private byte[] preclose = null;
	
	public byte[] getPreclose() {
		return preclose;
	}
	
	/** Before a close, this will attempt to be sent, if not null. */
	public void setPreclose(byte[] pc) {
		preclose = pc;
	}
	
	public void close() throws IOException {
		if (closed) return; // already closed
		if (preclose != null) {
			outBuf.append(preclose);
			flush(100L);
		}else if (!to) flush(100L); // wait for up to 100 ms
		closed = true;
		int s = CLib.close(sockfd);
		if (session > 0L) GNUTLS.close(session);
		if (s < 0) throw new CException(CLib.errno(), "socket failed close");
		callback.closed(this);
	}
	
	public PacketReceiver getCallback() {
		return callback;
	}
	
	public String toString() {
		return "UNIOSocket SOCKFD = " + sockfd;
	}
}
