package org.avuna.httpd.http.networking;

import java.io.IOException;
import java.net.SocketException;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.hosts.ITerminatable;
import org.avuna.httpd.http.ResponseGenerator;
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
				ResponsePacket outgoingResponse = incomingRequest.child;
				outgoingResponse.request = incomingRequest;
				boolean main = outgoingResponse.request.parent == null;
				String add = main ? "" : "-SUB";
				long benchStart = System.nanoTime();
				host.patchBus.processPacket(incomingRequest);
				if (incomingRequest.drop) {
					incomingRequest.work.s.close();
					Logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + add + " " + incomingRequest.host.getHostPath() + incomingRequest.target + " returned DROPPED took: " + (System.nanoTime() - benchStart) / 1000000D + " ms");
					continue;
				}
				boolean cont = ResponseGenerator.process(incomingRequest, outgoingResponse);
				if (cont) host.patchBus.processPacket(outgoingResponse);
				if (outgoingResponse.drop) {
					incomingRequest.work.s.close();
					Logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + add + " " + incomingRequest.host.getHostPath() + incomingRequest.target + " returned DROPPED took: " + (System.nanoTime() - benchStart) / 1000000D + " ms");
					continue;
				}
				if (main) outgoingResponse.prewrite();
				else outgoingResponse.subwrite();
				if (outgoingResponse.drop) {
					incomingRequest.work.s.close();
					Logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + add + " " + incomingRequest.host.getHostPath() + incomingRequest.target + " returned DROPPED took: " + (outgoingResponse.bwt - benchStart) / 1000000D + " ms");
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
				Logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + add + " " + incomingRequest.host.getHostPath() + incomingRequest.target + " returned " + outgoingResponse.statusCode + " " + outgoingResponse.reasonPhrase + " took: " + (outgoingResponse.bwt - benchStart) / 1000000D + " ms");
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
