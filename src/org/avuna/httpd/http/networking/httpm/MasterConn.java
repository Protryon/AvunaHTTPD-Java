package org.avuna.httpd.http.networking.httpm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class MasterConn {
	public final Socket s;
	public final DataInputStream in;
	public final DataOutputStream out;
	
	public MasterConn(Socket s, DataOutputStream out, DataInputStream in) {
		this.s = s;
		this.in = in;
		this.out = out;
	}
}
