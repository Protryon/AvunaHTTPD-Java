package org.avuna.httpd.hosts;

import java.io.File;
import java.net.ServerSocket;
import java.util.HashMap;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.dns.RecordHolder;
import org.avuna.httpd.dns.TCPServer;
import org.avuna.httpd.dns.ThreadDNSWorker;
import org.avuna.httpd.dns.UDPServer;

public class HostDNS extends Host {
	
	public HostDNS(String name, String ip, int port, boolean isSSL, File keyFile, String keyPassword, String keystorePassword) {
		super(name, ip, port, isSSL, keyFile, keyPassword, keystorePassword, Protocol.DNS);
	}
	
	private String ip = null, dnsf = null;
	private int twc, mc;
	private int port;
	
	public void formatConfig(HashMap<String, Object> map) {
		if (!map.containsKey("dnsf")) map.put("dnsf", AvunaHTTPD.fileManager.getBaseFile("dns.cfg").getAbsolutePath());
		dnsf = (String)map.get("dnsf");
		ip = (String)map.get("ip");
		port = Integer.parseInt((String)map.get("port"));
		if (!map.containsKey("workerThreadCount")) map.put("workerThreadCount", "8");
		if (!map.containsKey("maxConnections")) map.put("maxConnections", "-1");
		twc = Integer.parseInt((String)map.get("workerThreadCount"));
		mc = Integer.parseInt((String)map.get("maxConnections"));
	}
	
	public void setup(ServerSocket s) {
		RecordHolder holder = new RecordHolder(new File(dnsf));
		ThreadDNSWorker.holder = holder;
		ThreadDNSWorker.initQueue(mc < 1 ? 10000000 : mc);
		for (int i = 0; i < twc; i++) {
			ThreadDNSWorker worker = new ThreadDNSWorker();
			worker.setDaemon(true);
			worker.start();
		}
		UDPServer udp = new UDPServer();
		udp.start();
		TCPServer tcp = new TCPServer(s);
		tcp.start();
	}
}
