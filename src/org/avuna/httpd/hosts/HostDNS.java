package org.avuna.httpd.hosts;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.dns.RecordHolder;
import org.avuna.httpd.dns.TCPServer;
import org.avuna.httpd.dns.ThreadDNSWorker;
import org.avuna.httpd.dns.UDPServer;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.Logger;

public class HostDNS extends Host {
	
	public HostDNS(String name) {
		super(name, Protocol.DNS);
	}
	
	private String dnsf = null;
	private int twc, mc;
	
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
	
	public void setup(ServerSocket s) {
		RecordHolder holder = new RecordHolder(new File(dnsf));
		ThreadDNSWorker.holder = holder;
		ThreadDNSWorker.initQueue(mc < 1 ? 10000000 : mc);
		for (int i = 0; i < twc; i++) {
			ThreadDNSWorker worker = new ThreadDNSWorker();
			addTerm(worker);
			worker.setDaemon(true);
			worker.start();
		}
		UDPServer udp = new UDPServer();
		udp.start();
		TCPServer tcp = new TCPServer(s);
		tcp.start();
		while (!udp.bound)
			try {
				Thread.sleep(1L);
			}catch (InterruptedException e) {
				Logger.logError(e);
			}
	}
}
