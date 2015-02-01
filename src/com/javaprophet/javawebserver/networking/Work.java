package com.javaprophet.javawebserver.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class Work {
	public final Socket s;
	public final DataInputStream in;
	public final DataOutputStream out;
	public final boolean ssl;
	public int tos = 0;
	
	public Work(Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
		this.s = s;
		this.in = in;
		this.out = out;
		this.ssl = ssl;
	}
}