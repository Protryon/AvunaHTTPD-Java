package org.avuna.httpd.http.networking;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.hosts.HostHTTPM;
import org.avuna.httpd.util.Logger;

public class ThreadConnection extends Thread {
	private static int nid = 1;
	protected final HostHTTP host;
	
	public ThreadConnection(HostHTTP host) {
		super("Avuna " + (host instanceof HostHTTPM ? "HTTPM-" : "HTTP-") + "Connection Thread #" + nid++);
		this.host = host;
		host.conns.add(this);
	}
	
	protected boolean keepRunning = true;
	
	public void close() {
		keepRunning = false;
	}
	
	public void run() {
		while (keepRunning) {
			Work focus = host.pollQueue();
			if (focus == null) {
				try {
					Thread.sleep(1L);
				}catch (InterruptedException e) {
					Logger.logError(e);
				}
				continue;
			}
			if (focus.s.isClosed()) {
				String ip = focus.s.getInetAddress().getHostAddress();
				Integer cur = HostHTTP.connIPs.get(ip);
				if (cur == null) cur = 1;
				cur -= 1;
				HostHTTP.connIPs.put(ip, cur);
				Logger.log(ip + " closed.");
				continue;
			}
			boolean canAdd = true;
			boolean readd = false;
			try {
				ResponsePacket peek;
				while ((peek = focus.outQueue.peek()) != null && peek.done) {
					focus.outQueue.poll();
					boolean t = peek.reqTransfer;
					if (peek.reqStream != null) {
						ThreadJavaLoaderStreamWorker sw = new ThreadJavaLoaderStreamWorker(host, focus, peek.request, peek, peek.reqStream);
						host.subworkers.add(sw);
						sw.start();
						readd = false;
						canAdd = false;
					}else if (peek.toStream != null) {
						ThreadRawStreamWorker sw = new ThreadRawStreamWorker(host, focus, peek.request, peek, peek.toStream);
						host.subworkers.add(sw);
						sw.start();
						readd = false;
						canAdd = false;
					}else if (t && peek.body != null) {
						ThreadStreamWorker sw = new ThreadStreamWorker(host, focus, peek.request, peek);
						host.subworkers.add(sw);
						sw.start();
						readd = false;
						canAdd = false;
					}else {
						readd = true;
						focus.out.write(peek.subwrite);
						focus.out.flush();
					}
				}
				if (focus.ssl && focus.in.available() == 0) {
					focus.s.setSoTimeout(1);
					try {
						int sp = focus.in.read();
						if (sp == -1) {
							focus.s.close();
							readd = false;
							continue;
						}
						focus.sslprep.write(sp);
					}catch (SocketTimeoutException e) {
						
					}finally {
						focus.s.setSoTimeout(1000);
					}
				}
				if (focus.in.available() == 0 && !focus.blockTimeout) {
					if (focus.sns == 0L) {
						focus.sns = System.nanoTime() + 10000000000L;
						readd = true;
						if (host.emptyQueue()) {
							try {
								Thread.sleep(1L);
							}catch (InterruptedException e) {
								Logger.logError(e);
							}
						}
						continue;
					}else {
						if (focus.sns >= System.nanoTime()) {
							boolean sleep = host.emptyQueue();
							if (AvunaHTTPD.bannedIPs.contains(focus.s.getInetAddress().getHostAddress())) {
								focus.s.close();
							}else {
								readd = true;
							}
							if (sleep) {
								try {
									Thread.sleep(1L);
								}catch (InterruptedException e) {
									Logger.logError(e);
								}
							}
							continue;
						}else {
							readd = false;
							focus.s.close();
							String ip = focus.s.getInetAddress().getHostAddress();
							Integer cur = HostHTTP.connIPs.get(ip);
							if (cur == null) cur = 1;
							cur -= 1;
							HostHTTP.connIPs.put(ip, cur);
							Logger.log(ip + " closed.");
							continue;
						}
					}
				}else if (focus.in.available() > 0) {
					focus.sns = 0L;
					long ps = System.nanoTime();
					RequestPacket incomingRequest = RequestPacket.read(focus.sslprep != null ? focus.sslprep.toByteArray() : null, focus.in, host);
					if (focus.sslprep != null) focus.sslprep.reset();
					long benchStart = System.nanoTime();
					if (incomingRequest == null) {
						focus.s.close();
						continue;
					}
					
					String host = incomingRequest.headers.hasHeader("Host") ? incomingRequest.headers.getHeader("Host") : "";
					incomingRequest.work = focus;
					incomingRequest.host = focus.host.getVHost(host);
					incomingRequest.ssl = focus.ssl;
					focus.tos = 0;
					incomingRequest.userIP = focus.s.getInetAddress().getHostAddress();
					incomingRequest.userPort = focus.s.getPort();
					incomingRequest.order = focus.nreqid++;
					incomingRequest.child = new ResponsePacket();
					incomingRequest.child.request = incomingRequest;
					focus.outQueue.add(incomingRequest.child);
					long set = System.nanoTime();
					if (focus.rqst == 0L) {
						focus.rqst = System.currentTimeMillis();
					}
					focus.rqs++;
					this.host.addWork(incomingRequest);
					readd = true;
				}else if (focus.blockTimeout) {
					readd = true;
					focus.sns = 0L;
				}
			}catch (SocketTimeoutException e) {
				if (!focus.blockTimeout) {
					focus.tos++;
					if (focus.tos < 10) {
						readd = true;
					}else {
						// Logger.logError(e);
						try {
							focus.s.close();
						}catch (IOException ex) {
							Logger.logError(ex);
						}
						String ip = focus.s.getInetAddress().getHostAddress();
						Integer cur = HostHTTP.connIPs.get(ip);
						if (cur == null) cur = 1;
						cur -= 1;
						HostHTTP.connIPs.put(ip, cur);
						Logger.log(ip + " closed.");
						readd = false;
					}
				}else {
					readd = true;
					focus.sns = 0L;
				}
			}catch (Exception e) {
				if (!(e instanceof SocketTimeoutException)) {
					if (!(e instanceof SocketException || e instanceof StringIndexOutOfBoundsException)) Logger.logError(e);
					try {
						focus.s.close();
					}catch (IOException ex) {
						Logger.logError(ex);
					}
					String ip = focus.s.getInetAddress().getHostAddress();
					Integer cur = HostHTTP.connIPs.get(ip);
					if (cur == null) cur = 1;
					cur -= 1;
					HostHTTP.connIPs.put(ip, cur);
					Logger.log(ip + " closed.");
					readd = false;
				}
			}finally {
				if (readd & canAdd) {
					host.readdWork(focus);
				}
				if (host.sizeQueue() < 10000) { // idle fix
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
