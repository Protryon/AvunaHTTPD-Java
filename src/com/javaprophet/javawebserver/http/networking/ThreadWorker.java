package com.javaprophet.javawebserver.http.networking;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.http.ResponseGenerator;
import com.javaprophet.javawebserver.util.Logger;

public class ThreadWorker extends Thread {
	private static int nid = 1;
	
	public ThreadWorker() {
		super("JWS Worker Thread #" + nid++);
		setDaemon(true);
		workers.add(this);
	}
	
	public static void clearWork() {
		workQueue.clear();
	}
	
	private static ArrayList<ThreadWorker> workers = new ArrayList<ThreadWorker>();
	private static ArrayBlockingQueue<RequestPacket> workQueue;
	
	public static void addWork(RequestPacket req) {
		workQueue.add(req);
	}
	
	public static void initQueue() {
		workQueue = new ArrayBlockingQueue<RequestPacket>(1000000);
	}
	
	public static ResponsePacket[] processSubRequests(RequestPacket... reqs) {
		ResponsePacket[] resps = new ResponsePacket[reqs.length];
		for (int i = 0; i < resps.length; i++) {
			resps[i] = new ResponsePacket();
			reqs[i].child = resps[i];
			resps[i].request = reqs[i];
		}
		for (int i = 0; i < resps.length; i++) {
			addWork(reqs[i]);
		}
		major:
		while (true) {
			for (ResponsePacket resp : resps) {
				if (!resp.done) {
					try {
						Thread.sleep(0L, 100000); // TODO: longer? smarter?
					}catch (InterruptedException e) {
						Logger.logError(e);
					}
					continue major;
				}
			}
			break;
		}
		return resps;
	}
	
	private boolean keepRunning = true;
	
	public void close() {
		keepRunning = false;
	}
	
	public static int getQueueSize() {
		return workQueue.size();
	}
	
	public void run() {
		while (keepRunning) {
			RequestPacket incomingRequest = workQueue.poll();
			if (incomingRequest == null) {
				try {
					Thread.sleep(1L);
				}catch (InterruptedException e) {
					Logger.logError(e);
				}
				continue;
			}
			try {
				ResponsePacket outgoingResponse = incomingRequest.child;
				outgoingResponse.request = incomingRequest;
				boolean main = outgoingResponse.request.parent == null;
				String add = main ? "" : "-SUB";
				long benchStart = System.nanoTime();
				JavaWebServer.patchBus.processPacket(incomingRequest);
				if (incomingRequest.drop) {
					incomingRequest.work.s.close();
					Logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + add + " " + incomingRequest.target + " returned DROPPED took: " + (System.nanoTime() - benchStart) / 1000000D + " ms");
					continue;
				}
				long proc1 = System.nanoTime();
				boolean cont = ResponseGenerator.process(incomingRequest, outgoingResponse);
				long resp = System.nanoTime();
				if (cont) JavaWebServer.patchBus.processPacket(outgoingResponse);
				if (outgoingResponse.drop) {
					incomingRequest.work.s.close();
					Logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + add + " " + incomingRequest.target + " returned DROPPED took: " + (System.nanoTime() - benchStart) / 1000000D + " ms");
					continue;
				}
				long proc2 = System.nanoTime();
				if (main) outgoingResponse.prewrite();
				else outgoingResponse.subwrite();
				if (outgoingResponse.drop) {
					incomingRequest.work.s.close();
					Logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + add + " " + incomingRequest.target + " returned DROPPED took: " + (outgoingResponse.bwt - benchStart) / 1000000D + " ms");
					continue;
				}
				outgoingResponse.done = true;
				// Logger.log((benchStart - ps) / 1000000D + " ps-start");
				// Logger.log((set - benchStart) / 1000000D + " start-set");
				// Logger.log((proc1 - set) / 1000000D + " set-proc1");
				// Logger.log((resp - proc1) / 1000000D + " proc1-resp");
				// Logger.log((proc2 - resp) / 1000000D + " resp-proc2");
				// Logger.log((write - proc2) / 1000000D + " proc2-write");
				// Logger.log((cur - write) / 1000000D + " write-cur");
				if (incomingRequest.host.getDebug()) {
					Logger.log(JavaWebServer.crlf + incomingRequest.toString().trim());
				}
				Logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + add + " " + incomingRequest.target + " returned " + outgoingResponse.statusCode + " " + outgoingResponse.reasonPhrase + " took: " + (outgoingResponse.bwt - benchStart) / 1000000D + " ms");
			}catch (Exception e) {
				if (!(e instanceof SocketException || e instanceof StringIndexOutOfBoundsException)) {
					Logger.logError(e);
					
				}else {
					try {
						incomingRequest.work.s.close();
					}catch (IOException ex) {
						Logger.logError(ex);
					}
				}
			}
		}
	}
}
