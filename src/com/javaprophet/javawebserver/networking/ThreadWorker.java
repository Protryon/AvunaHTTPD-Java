package com.javaprophet.javawebserver.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.hosts.Host;
import com.javaprophet.javawebserver.http.ResponseGenerator;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.util.Logger;

public class ThreadWorker extends Thread {
	private static int nid = 1;
	
	public ThreadWorker() {
		super("JWS Worker Thread #" + nid++);
		workers.add(this);
	}
	
	public static void clearWork() {
		workQueue.clear();
	}
	
	private static ArrayList<ThreadWorker> workers = new ArrayList<ThreadWorker>();
	private static ArrayBlockingQueue<Work> workQueue;
	protected static HashMap<String, Integer> connIPs = new HashMap<String, Integer>();
	
	public static void initQueue(int connlimit) {
		workQueue = new ArrayBlockingQueue<Work>(connlimit);
	}
	
	public static int getConnectionsForIP(String ip) {
		return connIPs.containsKey(ip) ? connIPs.get(ip) : 0;
	}
	
	public static void addWork(Host host, Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
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
	
	public static final ArrayList<Thread> subworkers = new ArrayList<Thread>();
	
	public static int getQueueSize() {
		return workQueue.size();
	}
	
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
				if (!focus.s.isClosed() && focus.in.available() == 0) {
					if (focus.sns == 0L) {
						focus.sns = System.nanoTime() + 10000000000L;
						workQueue.add(focus);
						if (workQueue.isEmpty()) {
							try {
								Thread.sleep(10L);
							}catch (InterruptedException e) {
								Logger.logError(e);
							}
						}
						continue;
					}else {
						if (focus.sns >= System.nanoTime()) {
							boolean sleep = workQueue.isEmpty();
							if (JavaWebServer.bannedIPs.contains(focus.s.getInetAddress().getHostAddress())) {
								focus.s.close();
							}else {
								workQueue.add(focus);
							}
							if (sleep) {
								try {
									Thread.sleep(10L);
								}catch (InterruptedException e) {
									Logger.logError(e);
								}
							}
							continue;
						}else {
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
				}else if (!focus.s.isClosed()) { // TODO: fix pipelining?
					focus.sns = 0L;
					long ps = System.nanoTime();
					RequestPacket incomingRequest = RequestPacket.read(focus.in);
					long benchStart = System.nanoTime();
					if (incomingRequest == null) {
						focus.s.close();
						continue;
					}
					String host = incomingRequest.headers.hasHeader("Host") ? incomingRequest.headers.getHeader("Host") : "";
					incomingRequest.host = focus.host.getVHost(host);
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
					boolean cont = ResponseGenerator.process(incomingRequest, outgoingResponse);
					long resp = System.nanoTime();
					if (cont) JavaWebServer.patchBus.processPacket(outgoingResponse);
					if (outgoingResponse.drop) {
						focus.s.close();
						Logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + " " + incomingRequest.target + " returned DROPPED took: " + (System.nanoTime() - benchStart) / 1000000D + " ms");
						continue;
					}
					long proc2 = System.nanoTime();
					outgoingResponse.write(focus.out);
					if (outgoingResponse.drop) {
						focus.s.close();
						Logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + " " + incomingRequest.target + " returned DROPPED took: " + (outgoingResponse.bwt - benchStart) / 1000000D + " ms");
						continue;
					}
					boolean t = outgoingResponse.reqTransfer;
					long write = System.nanoTime();
					if (outgoingResponse.reqStream != null) {
						ThreadJavaLoaderStreamWorker sw = new ThreadJavaLoaderStreamWorker(focus, incomingRequest, outgoingResponse, outgoingResponse.reqStream);
						subworkers.add(sw);
						sw.start();
					}else if (t && outgoingResponse.body != null) {
						ThreadStreamWorker sw = new ThreadStreamWorker(focus, incomingRequest, outgoingResponse);
						subworkers.add(sw);
						sw.start();
					}else {
						workQueue.add(focus);
					}
					long cur = System.nanoTime();
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
					Logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + " " + incomingRequest.target + " returned " + outgoingResponse.statusCode + " " + outgoingResponse.reasonPhrase + " took: " + (outgoingResponse.bwt - benchStart) / 1000000D + " ms");
				}else {
					String ip = focus.s.getInetAddress().getHostAddress();
					Integer cur = connIPs.get(ip);
					if (cur == null) cur = 1;
					cur -= 1;
					connIPs.put(ip, cur);
					Logger.log(ip + " closed.");
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
					String ip = focus.s.getInetAddress().getHostAddress();
					Integer cur = connIPs.get(ip);
					if (cur == null) cur = 1;
					cur -= 1;
					connIPs.put(ip, cur);
					Logger.log(ip + " closed.");
				}
			}catch (Exception e) {
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
			}
		}
	}
}
