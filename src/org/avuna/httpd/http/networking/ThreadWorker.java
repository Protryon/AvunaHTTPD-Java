package org.avuna.httpd.http.networking;

import java.io.IOException;
import java.net.SocketException;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.http.ResponseGenerator;
import org.avuna.httpd.util.Logger;

public class ThreadWorker extends Thread {
	private static int nid = 1;
	private final HostHTTP host;
	
	public ThreadWorker(HostHTTP host) {
		super("Avuna HTTP-Worker Thread #" + nid++);
		this.host = host;
		host.workers.add(this);
	}
	
	private boolean keepRunning = true;
	
	public void close() {
		keepRunning = false;
	}
	
	public void run() {
		while (keepRunning) {
			RequestPacket incomingRequest = host.pollReqQueue();
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
				host.patchBus.processPacket(incomingRequest);
				if (incomingRequest.drop) {
					incomingRequest.work.s.close();
					Logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + add + " " + incomingRequest.target + " returned DROPPED took: " + (System.nanoTime() - benchStart) / 1000000D + " ms");
					continue;
				}
				long proc1 = System.nanoTime();
				boolean cont = ResponseGenerator.process(incomingRequest, outgoingResponse);
				long resp = System.nanoTime();
				if (cont) host.patchBus.processPacket(outgoingResponse);
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
					Logger.log(AvunaHTTPD.crlf + incomingRequest.toString().trim());
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
			}finally {
				if (host.sizeReqQueue() < 10000) { // idle fix
					try {
						Thread.sleep(0L, 100000);
					}catch (InterruptedException e) {
						Logger.logError(e);
					}
				}
			}
		}
	}
}
