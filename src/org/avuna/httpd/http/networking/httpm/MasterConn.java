package org.avuna.httpd.http.networking.httpm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import org.avuna.httpd.util.unixsocket.UnixSocket;

public class MasterConn {
	private final Socket s;
	private final UnixSocket us;
	private final DataOutputStream out;
	private final DataInputStream in;
	
	public MasterConn(Socket s) throws IOException {
		this.s = s;
		this.us = null;
		this.out = new DataOutputStream(s.getOutputStream());
		this.out.flush();
		this.in = new DataInputStream(s.getInputStream());
	}
	
	public MasterConn(UnixSocket us) throws IOException {
		this.us = us;
		this.s = null;
		this.out = new DataOutputStream(us.getOutputStream());
		this.out.flush();
		this.in = new DataInputStream(us.getInputStream());
	}
	
	public DataOutputStream getOutputStream() throws IOException {
		return out;
	}
	
	public DataInputStream getInputStream() throws IOException {
		return in;
	}
}
