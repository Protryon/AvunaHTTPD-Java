/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

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
			AvunaHTTPD.logger.logError(e);
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
		if (this.zf != null) try {
			this.zf.load(this);
		}catch (IOException e) {
			logger.logError(e);
		}
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
			this.zf.load(this);
		}catch (IOException e1) {
			logger.logError(e1);
			logger.log("Failed to read DNS zone file!");
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
				logger.logError(e);
			}
	}
}
