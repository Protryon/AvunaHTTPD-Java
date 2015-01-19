package com.javaprophet.javawebserver.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;

/**
 * Handles a single connection.
 */
public class ConnectionJWS extends Connection {
	
	public ConnectionJWS(Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
		super(s, in, out, ssl);
	}
	
	protected LinkedBlockingQueue<ThreadPipeline> pipeQueue = new LinkedBlockingQueue<ThreadPipeline>();
	protected LinkedBlockingQueue<ThreadPipeline> finishedPipeQueue = new LinkedBlockingQueue<ThreadPipeline>();
	private ThreadPipeflow pipeflow = new ThreadPipeflow(this);
	
	public void handleConnection() {
		Thread t = new Thread() {
			public void run() {
				process();
			}
		};
		t.start();
	}
	
	private void process() {
		try {
			s.setSoTimeout(10000);
		}catch (SocketException e1) {
			e1.printStackTrace();
		}
		if (!s.isClosed() && !closeWanted) {
			pipeflow.start();
		}
		while (!s.isClosed() && !closeWanted) {
			try {
				RequestPacket incomingRequest = RequestPacket.read(in);
				if (incomingRequest == null) {
					closeWanted = true;
					continue;
				}
				incomingRequest.userIP = s.getInetAddress().getHostAddress();
				incomingRequest.userPort = s.getPort();
				ResponsePacket outgoingResponse = new ResponsePacket();
				outgoingResponse.request = incomingRequest;
				ThreadPipeline pipe = new ThreadPipeline(this, incomingRequest, outgoingResponse);
				pipe.start();
				pipeQueue.add(pipe);
			}catch (Exception ex) {
				ex.printStackTrace();
				try {
					s.close();
				}catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (!s.isClosed()) {
			try {
				s.close();
			}catch (IOException e) {
				e.printStackTrace();
			}
		}
		JavaWebServer.runningThreads.remove(this);
	}
}
