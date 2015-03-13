package com.javaprophet.javawebserver.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import com.javaprophet.javawebserver.hosts.Host;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;

public class Work {
	public final Socket s;
	public final DataInputStream in;
	public final DataOutputStream out;
	public final boolean ssl;
	public final Host host;
	public int tos = 0;
	public long sns = 0L;
	public int nreqid = 1;
	
	public ArrayBlockingQueue<ResponsePacket> outQueue = new ArrayBlockingQueue<ResponsePacket>(16);
	
	// public ResponsePacket[] pipeline = new ResponsePacket[32];
	
	public Work(Host host, Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
		this.host = host;
		this.s = s;
		this.in = in;
		this.out = out;
		this.ssl = ssl;
	}
}