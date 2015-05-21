package org.avuna.httpd.dns;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class WorkTCP extends Work {
	protected final Socket s;
	protected final DataInputStream in;
	protected final DataOutputStream out;
	
	public WorkTCP(Socket s, DataInputStream in, DataOutputStream out) {
		super(false);
		this.s = s;
		this.in = in;
		this.out = out;
	}
}
