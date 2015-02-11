package com.javaprophet.javawebserver.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import com.javaprophet.javawebserver.hosts.Host;

public class Work {
	public final Socket s;
	public final DataInputStream in;
	public final DataOutputStream out;
	public final boolean ssl;
	public final Host host;
	public int tos = 0;
	public long sns = 0L;
	
	public Work(Host host, Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
		this.host = host;
		this.s = s;
		this.in = in;
		this.out = out;
		this.ssl = ssl;
	}
}