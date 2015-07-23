/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.networking;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.event.base.EventDisconnected;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.hosts.HostHTTPM;
import org.avuna.httpd.hosts.ITerminatable;
import org.avuna.httpd.util.Logger;

public class ThreadConnection extends Thread implements ITerminatable {
	private static int nid = 1;
	protected final HostHTTP host;
	
	public ThreadConnection(HostHTTP host) {
		super("Avuna " + (host instanceof HostHTTPM ? "HTTPM-" : "HTTP-") + "Connection Thread #" + nid++);
		this.host = host;
		host.conns.add(this);
	}
	
	protected boolean keepRunning = true;
	
	public void run() {
		while (keepRunning) {
			Work focus = host.getWork();
			if (focus == null) {
				try {
					Thread.sleep(5L);
				}catch (InterruptedException e) {
					// Logger.logError(e);
				}
				continue;
			}
			if (focus.s.isClosed()) {
				host.eventBus.callEvent(new EventDisconnected(focus));
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
						ThreadRawStreamWorker sw = new ThreadRawStreamWorker(host, focus, peek, peek.toStream);
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
						continue;
					}else {
						if (focus.sns >= System.nanoTime()) {
							if (AvunaHTTPD.bannedIPs.contains(focus.s.getInetAddress().getHostAddress())) {
								focus.close();
							}else {
								readd = true;
							}
							continue;
						}else {
							readd = false;
							focus.close();
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
					RequestPacket incomingRequest = RequestPacket.read(focus.sslprep != null ? focus.sslprep.toByteArray() : null, focus.in, host);
					if (focus.sslprep != null) focus.sslprep.reset();
					if (incomingRequest == null) {
						focus.close();
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
							focus.close();
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
						focus.close();
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
				if (!readd || !canAdd) {
					host.removeWork(focus);
				}else {
					focus.inUse = false;
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
