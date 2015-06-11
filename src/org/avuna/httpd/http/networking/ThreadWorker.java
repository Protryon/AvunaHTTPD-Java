package org.avuna.httpd.http.networking;

import java.io.IOException;
import java.net.SocketException;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.hosts.ITerminatable;
import org.avuna.httpd.http.ResponseGenerator;
import org.avuna.httpd.http.event.EventGenerateResponse;
import org.avuna.httpd.http.event.EventPreprocessRequest;
import org.avuna.httpd.http.event.EventResponseFinished;
import org.avuna.httpd.http.event.EventResponseSent;
import org.avuna.httpd.util.Benchmark;
import org.avuna.httpd.util.Logger;

public class ThreadWorker extends Thread implements ITerminatable {
	protected static int nid = 1;
	protected final HostHTTP host;
	
	public ThreadWorker(HostHTTP host) {
		super("Avuna HTTP-Worker Thread #" + nid++);
		this.host = host;
	}
	
	protected boolean keepRunning = true;
	
	public void run() {
		while (keepRunning) {
			RequestPacket incomingRequest = host.pollReqQueue();
			if (incomingRequest == null) {
				try {
					synchronized (this) {
						this.wait();
					}
				}catch (InterruptedException e) {
					// Logger.logError(e);
				}
				continue;
			}
			try {
				Benchmark bm = new Benchmark(false);
				bm.startSection("req");
				bm.startSection("head");
				ResponsePacket outgoingResponse = incomingRequest.child;
				outgoingResponse.request = incomingRequest;
				boolean main = outgoingResponse.request.parent == null;
				String add = main ? "" : "-SUB";
				long benchStart = System.nanoTime();
				bm.endSection("head");
				bm.startSection("preproc");
				EventPreprocessRequest epr = new EventPreprocessRequest(incomingRequest);
				host.eventBus.callEvent(epr);
				if (incomingRequest.drop || epr.isCanceled()) {
					incomingRequest.work.close();
					Logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + add + " " + incomingRequest.host.getHostPath() + incomingRequest.target + " DROPPED took: " + (System.nanoTime() - benchStart) / 1000000D + " ms");
					continue;
				}
				bm.endSection("preproc");
				bm.startSection("rg");
				ResponseGenerator.process(incomingRequest, outgoingResponse);
				bm.endSection("rg");
				bm.startSection("gen-resp");
				EventGenerateResponse epr2 = new EventGenerateResponse(incomingRequest, outgoingResponse);
				host.eventBus.callEvent(epr2);
				bm.endSection("gen-resp");
				bm.startSection("resp-finished");
				EventResponseFinished epr3 = new EventResponseFinished(outgoingResponse);
				host.eventBus.callEvent(epr3);
				if (outgoingResponse.drop) {
					incomingRequest.work.close();
					Logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + add + " " + incomingRequest.host.getHostPath() + incomingRequest.target + " DROPPED took: " + (System.nanoTime() - benchStart) / 1000000D + " ms");
					continue;
				}
				bm.endSection("resp-finished");
				bm.startSection("prewrite");
				if (main) outgoingResponse.prewrite();
				else outgoingResponse.subwrite();
				if (outgoingResponse.drop) {
					incomingRequest.work.close();
					Logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + add + " " + incomingRequest.host.getHostPath() + incomingRequest.target + " DROPPED took: " + (outgoingResponse.bwt - benchStart) / 1000000D + " ms");
					continue;
				}
				bm.endSection("prewrite");
				bm.startSection("post");
				
				outgoingResponse.done = true;
				host.eventBus.callEvent(new EventResponseSent(outgoingResponse));
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
				Logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + add + " " + incomingRequest.host.getHostPath() + incomingRequest.target + " returned " + outgoingResponse.statusCode + " " + outgoingResponse.reasonPhrase + " took: " + (outgoingResponse.bwt - benchStart) / 1000000D + " ms");
				bm.endSection("post");
				bm.endSection("req");
				bm.log();
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
				if (host.sizeReqQueue() < 10000) {
					try {
						Thread.sleep(0L, 100000);
					}catch (InterruptedException e) {
						// Logger.logError(e);
					}
				}
			}
		}
	}
	
	@Override
	public void terminate() {
		keepRunning = false;
		this.interrupt();
	}
}
