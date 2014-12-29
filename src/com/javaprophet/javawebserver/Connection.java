package com.javaprophet.javawebserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Connection extends Thread {
	public final Socket s;
	public final DataInputStream in;
	public final DataOutputStream out;
	public static final ResponseGenerator rg = new ResponseGenerator();
	
	public Connection(Socket s, DataInputStream in, DataOutputStream out) {
		this.s = s;
		this.in = in;
		this.out = out;
	}
	
	public void run() {
		while (!s.isClosed()) {
			try {
				RequestPacket incomingRequest = RequestPacket.read(in);
				if (incomingRequest == null) {
					s.close();
					return;
				}
				System.out.println(incomingRequest.toString());
				ResponsePacket outgoingResponse = new ResponsePacket();
				rg.process(incomingRequest, outgoingResponse); // TODO: pipelining queue
				System.out.println(outgoingResponse.toString());
				outgoingResponse.write(out);
			}catch (Exception ex) {
				ex.printStackTrace();
				try {
					s.close();
				}catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		JavaWebServer.runningThreads.remove(this);
	}
}
