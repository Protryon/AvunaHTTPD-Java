/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.hosts.ITerminatable;
import org.avuna.httpd.hosts.VHost;
import org.avuna.httpd.http.Resource;
import org.avuna.httpd.http.ResponseGenerator;
import org.avuna.httpd.http.event.EventGenerateResponse;
import org.avuna.httpd.http.event.EventPreprocessRequest;
import org.avuna.httpd.http.event.EventResponseFinished;
import org.avuna.httpd.http.event.EventResponseSent;
import org.avuna.httpd.util.Benchmark;
import org.avuna.httpd.util.Stream;
import org.avuna.httpd.util.unixsocket.UnixSocket;

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
				}catch (InterruptedException e) {}
				continue;
			}
			try {
				if (incomingRequest.host == null) {
					incomingRequest.work.close(); // TODO 500 ISE
					continue;
				}
				if (incomingRequest.host.isForwarding()) {
					VHost vh = incomingRequest.host;
					if (incomingRequest.work.fn == null) {
						incomingRequest.work.fn = vh.isForwardUnix() ? new UnixSocket(vh.getForwardIP()) : new Socket(vh.getForwardIP(), vh.getForwardPort());
						incomingRequest.work.fnout = new DataOutputStream(incomingRequest.work.fn.getOutputStream());
						incomingRequest.work.fnout.flush();
						incomingRequest.work.fnin = new DataInputStream(incomingRequest.work.fn.getInputStream());
					}
					incomingRequest.headers.addHeader("X-Forwarded-For", incomingRequest.work.s.getInetAddress().getHostAddress());
					incomingRequest.write(incomingRequest.work.fnout);
					incomingRequest.work.fnout.flush();
					ResponsePacket outgoingResponse = incomingRequest.child;
					outgoingResponse.request = incomingRequest;
					String line = Stream.readLine(incomingRequest.work.fnin);
					if (line == null) {
						incomingRequest.work.close();
						continue;
					}
					if (line.length() == 0) {
						line = Stream.readLine(incomingRequest.work.fnin);
					}
					int i = line.indexOf(" ");
					outgoingResponse.httpVersion = line.substring(0, i);
					i++;
					outgoingResponse.statusCode = Integer.parseInt(line.substring(i, (i = line.indexOf(" ", i))));
					i++;
					outgoingResponse.reasonPhrase = line.substring(i);
					while ((line = Stream.readLine(incomingRequest.work.fnin)).length() > 0) {
						outgoingResponse.headers.addHeader(line);
					}
					
					if (outgoingResponse.headers.hasHeader("Content-Length")) {
						byte[] data = new byte[Integer.parseInt(outgoingResponse.headers.getHeader("Content-Length"))];
						incomingRequest.work.fnin.readFully(data);
						// readLine(incomingRequest.work.cn.in);
						outgoingResponse.body = new Resource(data, outgoingResponse.headers.hasHeader("Content-Type") ? outgoingResponse.headers.getHeader("Content-Type") : "text/html; charset=utf-8");
						outgoingResponse.prewrite();
						outgoingResponse.done = true;
						outgoingResponse.bwt = System.nanoTime();
					}else if (outgoingResponse.headers.hasHeader("Transfer-Encoding") && outgoingResponse.headers.getHeader("Transfer-Encoding").contains("chunked")) {
						outgoingResponse.toStream = new DataInputStream(new ChunkedInputStream(incomingRequest.work.fnin, outgoingResponse.headers.hasHeader("Content-Encoding") && outgoingResponse.headers.getHeader("Content-Encoding").equals("gzip")));
						outgoingResponse.done = true;
						outgoingResponse.bwt = System.nanoTime();
					}else {
						outgoingResponse.bwt = System.nanoTime();
						// no body
					}
				}else {
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
					incomingRequest.host.eventBus.callEvent(epr);
					if (incomingRequest.drop || epr.isCanceled()) {
						incomingRequest.work.close();
						incomingRequest.host.logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + add + " " + incomingRequest.host.getHostPath() + incomingRequest.target + " DROPPED took: " + (System.nanoTime() - benchStart) / 1000000D + " ms");
						continue;
					}
					bm.endSection("preproc");
					bm.startSection("rg");
					ResponseGenerator.process(incomingRequest, outgoingResponse);
					bm.endSection("rg");
					bm.startSection("gen-resp");
					EventGenerateResponse epr2 = new EventGenerateResponse(incomingRequest, outgoingResponse);
					incomingRequest.host.eventBus.callEvent(epr2);
					if (epr2.isCanceled() || outgoingResponse.drop) {
						incomingRequest.work.close();
						incomingRequest.host.logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + add + " " + incomingRequest.host.getHostPath() + incomingRequest.target + " DROPPED took: " + (System.nanoTime() - benchStart) / 1000000D + " ms");
						continue;
					}
					bm.endSection("gen-resp");
					bm.startSection("resp-finished");
					EventResponseFinished epr3 = new EventResponseFinished(outgoingResponse);
					incomingRequest.host.eventBus.callEvent(epr3);
					if (epr3.isCanceled() || outgoingResponse.drop) {
						incomingRequest.work.close();
						incomingRequest.host.logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + add + " " + incomingRequest.host.getHostPath() + incomingRequest.target + " DROPPED took: " + (System.nanoTime() - benchStart) / 1000000D + " ms");
						continue;
					}
					bm.endSection("resp-finished");
					bm.startSection("prewrite");
					if (main) outgoingResponse.prewrite();
					else outgoingResponse.subwrite();
					if (outgoingResponse.drop) {
						incomingRequest.work.close();
						incomingRequest.host.logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + add + " " + incomingRequest.host.getHostPath() + incomingRequest.target + " DROPPED took: " + (outgoingResponse.bwt - benchStart) / 1000000D + " ms");
						continue;
					}
					bm.endSection("prewrite");
					bm.startSection("post");
					outgoingResponse.done = true;
					if (host.unio() && main) {
						ResponsePacket peek;
						Work focus = incomingRequest.work;
						while ((peek = focus.outQueue.peek()) != null && peek.done) {
							focus.outQueue.poll();
							boolean t = peek.reqTransfer;
							if (peek.reqStream != null) {
								ThreadJavaLoaderStreamWorker sw = new ThreadJavaLoaderStreamWorker(host, focus, peek.request, peek, peek.reqStream);
								host.subworkers.add(sw);
								sw.start();
							}else if (peek.toStream != null) {
								ThreadRawStreamWorker sw = new ThreadRawStreamWorker(host, focus, peek, peek.toStream);
								host.subworkers.add(sw);
								sw.start();
							}else if (t && peek.body != null) {
								ThreadStreamWorker sw = new ThreadStreamWorker(host, focus, peek.request, peek);
								host.subworkers.add(sw);
								sw.start();
							}else {
								focus.out.write(peek.subwrite);
								// TODO: remove from input?
							}
						}
					}
					incomingRequest.host.eventBus.callEvent(new EventResponseSent(outgoingResponse));
					// Logger.log((benchStart - ps) / 1000000D + " ps-start");
					// Logger.log((set - benchStart) / 1000000D + " start-set");
					// Logger.log((proc1 - set) / 1000000D + " set-proc1");
					// Logger.log((resp - proc1) / 1000000D + " proc1-resp");
					// Logger.log((proc2 - resp) / 1000000D + " resp-proc2");
					// Logger.log((write - proc2) / 1000000D + " proc2-write");
					// Logger.log((cur - write) / 1000000D + " write-cur");
					if (incomingRequest.host.getDebug()) {
						incomingRequest.host.logger.log(AvunaHTTPD.crlf + incomingRequest.toString().trim());
					}
					incomingRequest.host.logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + add + " " + incomingRequest.host.getHostPath() + incomingRequest.target + " returned " + outgoingResponse.statusCode + " " + outgoingResponse.reasonPhrase + " took: " + (outgoingResponse.bwt - benchStart) / 1000000D + " ms");
					bm.endSection("post");
					bm.endSection("req");
					bm.log();
				}
			}catch (Exception e) {
				if (!(e instanceof SocketException)) {
					incomingRequest.host.logger.logError(e);
				}else {
					try {
						incomingRequest.work.s.close();
					}catch (IOException ex) {
						incomingRequest.host.logger.logError(ex);
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
