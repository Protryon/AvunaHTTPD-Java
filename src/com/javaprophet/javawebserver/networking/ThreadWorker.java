package com.javaprophet.javawebserver.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.util.Logger;

public class ThreadWorker extends Thread {
	
	public ThreadWorker() {
		
	}
	
	public static void clearWork() {
		workQueue.clear();
	}
	
	private static LinkedBlockingQueue<Work> workQueue = new LinkedBlockingQueue<Work>();
	
	public static void addWork(Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
		workQueue.add(new Work(s, in, out, ssl));
	}
	
	public static void readdWork(Work work) {
		workQueue.add(work);
	}
	
	private boolean keepRunning = true;
	
	public void close() {
		keepRunning = false;
	}
	
	public static final ArrayList<Thread> subworkers = new ArrayList<Thread>();
	
	public void run() {
		while (keepRunning) {
			Work focus = workQueue.poll();
			if (focus == null) {
				try {
					Thread.sleep(10L);
				}catch (InterruptedException e) {
					Logger.logError(e);
				}
				continue;
			}
			try {
				if (!focus.s.isClosed()) {
					RequestPacket incomingRequest = RequestPacket.read(focus.in);
					long benchStart = System.nanoTime();
					if (incomingRequest == null) {
						focus.s.close();
						continue;
					}
					incomingRequest.ssl = focus.ssl;
					focus.tos = 0;
					incomingRequest.userIP = focus.s.getInetAddress().getHostAddress();
					incomingRequest.userPort = focus.s.getPort();
					long set = System.nanoTime();
					JavaWebServer.patchBus.processPacket(incomingRequest);
					if (incomingRequest.drop) {
						focus.s.close();
						Logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + " " + incomingRequest.target + " returned DROPPED took: " + (System.nanoTime() - benchStart) / 1000000D + " ms");
						continue;
					}
					ResponsePacket outgoingResponse = new ResponsePacket();
					outgoingResponse.request = incomingRequest;
					long proc1 = System.nanoTime();
					boolean cont = JavaWebServer.rg.process(incomingRequest, outgoingResponse);
					long resp = System.nanoTime();
					if (cont) JavaWebServer.patchBus.processPacket(outgoingResponse);
					if (outgoingResponse.drop) {
						focus.s.close();
						Logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + " " + incomingRequest.target + " returned DROPPED took: " + (System.nanoTime() - benchStart) / 1000000D + " ms");
						continue;
					}
					long proc2 = System.nanoTime();
					ResponsePacket wrp = outgoingResponse.write(focus.out);
					if (wrp.drop) {
						focus.s.close();
						Logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + " " + incomingRequest.target + " returned DROPPED took: " + (System.nanoTime() - benchStart) / 1000000D + " ms");
						continue;
					}
					boolean t = wrp.reqTransfer;
					long write = System.nanoTime();
					if (wrp.reqStream != null) {
						ThreadJavaLoaderStreamWorker sw = new ThreadJavaLoaderStreamWorker(focus, incomingRequest, wrp, wrp.reqStream);
						subworkers.add(sw);
						sw.start();
					}else if (t && wrp.body != null && wrp.body.getBody() != null) {
						ThreadStreamWorker sw = new ThreadStreamWorker(focus, incomingRequest, wrp);
						subworkers.add(sw);
						sw.start();
					}else {
						workQueue.add(focus);
					}
					// Logger.log((set - benchStart) / 1000000D + " start-set");
					// Logger.log((proc1 - set) / 1000000D + " set-proc1");
					// Logger.log((resp - proc1) / 1000000D + " proc1-resp");
					// Logger.log((proc2 - resp) / 1000000D + " resp-proc2");
					// Logger.log((write - proc2) / 1000000D + " proc2-write");
					// Logger.log((cur - write) / 1000000D + " write-cur");
					Logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + " " + incomingRequest.target + " returned " + wrp.statusCode + " " + wrp.reasonPhrase + " took: " + (wrp.bwt - benchStart) / 1000000D + " ms");
				}else {
					Logger.log(focus.s.getInetAddress().getHostAddress() + " closed.");
				}
			}catch (SocketTimeoutException e) {
				focus.tos++;
				if (focus.tos < 10) {
					workQueue.add(focus);
				}else {
					// Logger.logError(e);
					try {
						focus.s.close();
					}catch (IOException ex) {
						Logger.logError(ex);
					}
					Logger.log(focus.s.getInetAddress().getHostAddress() + " closed.");
				}
			}catch (IOException e) {
				if (!(e instanceof SocketException)) Logger.logError(e);
				try {
					focus.s.close();
				}catch (IOException ex) {
					Logger.logError(ex);
				}
				Logger.log(focus.s.getInetAddress().getHostAddress() + " closed.");
			}
		}
	}
}
