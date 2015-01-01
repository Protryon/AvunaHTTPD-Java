package com.javaprophet.javawebserver.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;

public abstract class Connection {
	public final Socket s;
	public final DataInputStream in;
	public final DataOutputStream out;
	public final boolean ssl;
	public static final SimpleDateFormat timestamp = new SimpleDateFormat("HH:mm:ss");
	
	public Connection(Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
		this.s = s;
		this.in = in;
		this.out = out;
		this.ssl = ssl;
	}
	
	public void handleConnection() {
		
	}
	
	protected boolean closeWanted = false;
	
	public void close() throws IOException {
		closeWanted = true;
	}
}
