package org.avuna.httpd.http.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.http.Method;
import org.avuna.httpd.util.Logger;

public class ThreadConnection extends Thread {
	private static int nid = 1;
	
	public ThreadConnection() {
		super("Avuna Connection Thread #" + nid++);
		conns.add(this);
	}
	
	public static void clearWork() {
		workQueue.clear();
	}
	
	private static ArrayList<ThreadConnection> conns = new ArrayList<ThreadConnection>();
	private static ArrayBlockingQueue<Work> workQueue;
	protected static HashMap<String, Integer> connIPs = new HashMap<String, Integer>();
	
	public static void initQueue(int connlimit) {
		workQueue = new ArrayBlockingQueue<Work>(connlimit);
	}
	
	public static int getConnectionsForIP(String ip) {
		return connIPs.containsKey(ip) ? connIPs.get(ip) : 0;
	}
	
	public static void addWork(HostHTTP host, Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
		String ip = s.getInetAddress().getHostAddress();
		Integer cur = connIPs.get(ip);
		if (cur == null) cur = 0;
		cur += 1;
		connIPs.put(ip, cur);
		workQueue.add(new Work(host, s, in, out, ssl));
		Logger.log(ip + " connected to " + host.getHostname() + ".");
	}
	
	public static void clearIPs(String ip) {
		for (Object worko : workQueue.toArray()) {
			Work work = (Work)worko;
			if (work.s.getInetAddress().getHostAddress().equals(ip)) {
				workQueue.remove(work);
			}
		}
	}
	
	public static void readdWork(Work work) {
		workQueue.add(work);
	}
	
	private boolean keepRunning = true;
	
	public void close() {
		keepRunning = false;
	}
	
	public static int getQueueSize() {
		return workQueue.size();
	}
	
	public static final ArrayList<Thread> subworkers = new ArrayList<Thread>();
	
	public void run() {
		while (keepRunning) {
			Work focus = workQueue.poll();
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
				Integer cur = connIPs.get(ip);
				if (cur == null) cur = 1;
				cur -= 1;
				connIPs.put(ip, cur);
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
						ThreadJavaLoaderStreamWorker sw = new ThreadJavaLoaderStreamWorker(focus, peek.request, peek, peek.reqStream);
						subworkers.add(sw);
						sw.start();
						readd = false;
						canAdd = false;
					}else if (t && peek.body != null) {
						ThreadStreamWorker sw = new ThreadStreamWorker(focus, peek.request, peek);
						subworkers.add(sw);
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
						if (workQueue.isEmpty()) {
							try {
								Thread.sleep(1L);
							}catch (InterruptedException e) {
								Logger.logError(e);
							}
						}
						continue;
					}else {
						if (focus.sns >= System.nanoTime()) {
							boolean sleep = workQueue.isEmpty();
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
							Integer cur = connIPs.get(ip);
							if (cur == null) cur = 1;
							cur -= 1;
							connIPs.put(ip, cur);
							Logger.log(ip + " closed.");
							continue;
						}
					}
				}else if (focus.in.available() > 0) { // TODO: fix pipelining?
					focus.sns = 0L;
					long ps = System.nanoTime();
					RequestPacket incomingRequest = RequestPacket.read(focus.sslprep != null ? focus.sslprep.toByteArray() : null, focus.in);
					if (focus.sslprep != null) focus.sslprep.reset();
					long benchStart = System.nanoTime();
					if (incomingRequest == null) {
						focus.s.close();
						continue;
					}
					if (incomingRequest.method == Method.PRI && incomingRequest.target.equals("*") && incomingRequest.httpVersion.equals("HTTP/2.0")) {
						
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
					if (incomingRequest.host.getHost().http2) {
						if (incomingRequest.ssl) {
							
						}else {
							if (incomingRequest.headers.hasHeader("Upgrade") && incomingRequest.headers.hasHeader("HTTP2-Settings") && incomingRequest.headers.getHeader("Upgrade").contains("h2c")) {
								incomingRequest.http2Upgrade = true;
							}
						}
					}
					focus.outQueue.add(incomingRequest.child);
					long set = System.nanoTime();
					// code
					ThreadWorker.addWork(incomingRequest);
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
						Integer cur = connIPs.get(ip);
						if (cur == null) cur = 1;
						cur -= 1;
						connIPs.put(ip, cur);
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
					Integer cur = connIPs.get(ip);
					if (cur == null) cur = 1;
					cur -= 1;
					connIPs.put(ip, cur);
					Logger.log(ip + " closed.");
					readd = false;
				}
			}finally {
				if (readd & canAdd) {
					workQueue.add(focus);
				}
				if (workQueue.size() < 10000) { // idle fix
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
