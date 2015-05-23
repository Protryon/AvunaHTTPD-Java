package org.avuna.httpd.hosts;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.dns.TCPServer;
import org.avuna.httpd.dns.ThreadDNSWorker;
import org.avuna.httpd.dns.UDPServer;
import org.avuna.httpd.dns.Work;
import org.avuna.httpd.dns.zone.ZoneFile;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.Logger;

public class HostDNS extends Host {
	
	public HostDNS(String name) {
		super(name, Protocol.DNS);
	}
	
	private String dnsf = null;
	private int twc, mc;
	private ZoneFile zf;
	
	public ZoneFile getZone() {
		return zf;
	}
	
	public static void unpack() {
		try {
			AvunaHTTPD.fileManager.getBaseFile("dns.cfg").createNewFile();
		}catch (IOException e) {
			Logger.logError(e);
		}
	}
	
	public void formatConfig(ConfigNode map) {
		if (!map.containsNode("port")) map.insertNode("port", "53", "bind port for UDP & TCP");
		if (!map.containsNode("ip")) map.insertNode("ip", "0.0.0.0", "bind ip");
		if (!map.containsNode("dnsf")) map.insertNode("dnsf", AvunaHTTPD.fileManager.getBaseFile("dns.cfg").getAbsolutePath(), "dns zone file");
		dnsf = map.getNode("dnsf").getValue();
		if (!map.containsNode("workerThreadCount")) map.insertNode("workerThreadCount", "8", "dns worker thread count");
		if (!map.containsNode("maxConnections")) map.insertNode("maxConnections", "-1", "max number of *TCP* connections");
		twc = Integer.parseInt(map.getNode("workerThreadCount").getValue());
		mc = Integer.parseInt(map.getNode("maxConnections").getValue());
	}
	
	public ArrayBlockingQueue<Work> workQueue;
	
	public void clearWork() {
		workQueue.clear();
	}
	
	private HashMap<String, Integer> connIPs = new HashMap<String, Integer>();
	public ArrayList<ThreadDNSWorker> workers = new ArrayList<ThreadDNSWorker>();
	
	public void initQueue(int connlimit) {
		workQueue = new ArrayBlockingQueue<Work>(connlimit);
	}
	
	public int getConnectionsForIP(String ip) {
		return connIPs.get(ip);
	}
	
	public void addWork(Work work) {
		workQueue.add(work);
	}
	
	public int getQueueSize() {
		return workQueue.size();
	}
	
	public void setup(ServerSocket s) {
		this.zf = new ZoneFile(new File(dnsf));
		try {
			this.zf.load();
		}catch (IOException e1) {
			Logger.logError(e1);
			Logger.log("Failed to read DNS zone file!");
		}
		initQueue(mc < 1 ? 10000000 : mc);
		for (int i = 0; i < twc; i++) {
			ThreadDNSWorker worker = new ThreadDNSWorker(this);
			addTerm(worker);
			worker.setDaemon(true);
			worker.start();
		}
		UDPServer udp = new UDPServer(this);
		udp.start();
		addTerm(udp);
		TCPServer tcp = new TCPServer(this, s);
		tcp.start();
		addTerm(tcp);
		while (!udp.bound)
			try {
				Thread.sleep(1L);
			}catch (InterruptedException e) {
				Logger.logError(e);
			}
	}
}
