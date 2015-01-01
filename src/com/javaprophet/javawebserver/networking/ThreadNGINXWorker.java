package com.javaprophet.javawebserver.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;

public class ThreadNGINXWorker extends Thread {
	
	public ThreadNGINXWorker() {
		
	}
	
	private static class Work {
		public final Socket s;
		public final DataInputStream in;
		public final DataOutputStream out;
		public final boolean ssl;
		
		public Work(Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
			this.s = s;
			this.in = in;
			this.out = out;
			this.ssl = ssl;
		}
	}
	
	public static void clearWork() {
		workQueue.clear();
	}
	
	private static LinkedBlockingQueue<Work> workQueue = new LinkedBlockingQueue<Work>();
	
	public static void addWork(Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
		workQueue.add(new Work(s, in, out, ssl));
	}
	
	private boolean keepRunning = true;
	
	public void close() {
		keepRunning = false;
	}
	
	public void run() {
		while (keepRunning) {
			Work focus = workQueue.poll();
			if (focus == null) {
				try {
					Thread.sleep(10L);
				}catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
			try {
				if (!focus.s.isClosed()) {
					RequestPacket incomingRequest = RequestPacket.read(focus.in);
					if (incomingRequest == null) {
						focus.s.close();
						continue;
					}
					incomingRequest.userIP = focus.s.getInetAddress().getHostAddress();
					incomingRequest.userPort = focus.s.getPort();
					ResponsePacket outgoingResponse = new ResponsePacket();
					outgoingResponse.request = incomingRequest;
					JavaWebServer.patchBus.processPacket(incomingRequest);
					JavaWebServer.rg.process(incomingRequest, outgoingResponse);
					JavaWebServer.patchBus.processPacket(outgoingResponse);
					outgoingResponse.write(focus.out);
					workQueue.add(focus);
					System.out.println("[" + Connection.timestamp.format(new Date()) + "]" + incomingRequest.userIP + " requested " + incomingRequest.target + " returned " + outgoingResponse.statusCode + " " + outgoingResponse.reasonPhrase);
				}
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
