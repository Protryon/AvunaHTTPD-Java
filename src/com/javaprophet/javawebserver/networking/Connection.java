package com.javaprophet.javawebserver.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.http.ResponseGenerator;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;

/**
 * Handles a single connection.
 */
public class Connection extends Thread {
	public final Socket s;
	public final DataInputStream in;
	public final DataOutputStream out;
	public static final ResponseGenerator rg = new ResponseGenerator();
	public final boolean ssl;
	
	public Connection(Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
		this.s = s;
		this.in = in;
		this.out = out;
		this.ssl = ssl;
	}
	
	private boolean closeWanted = false;
	
	protected LinkedBlockingQueue<ThreadPipeline> pipeQueue = new LinkedBlockingQueue<ThreadPipeline>();
	protected LinkedBlockingQueue<ThreadPipeline> finishedPipeQueue = new LinkedBlockingQueue<ThreadPipeline>();
	private ThreadPipeflow pipeflow = new ThreadPipeflow(this);
	
	public void run() {
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
	
	public void close() throws IOException {
		closeWanted = true;
	}
}
