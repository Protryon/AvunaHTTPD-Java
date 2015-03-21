package org.avuna.httpd.hosts;

import java.io.File;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.http.networking.ThreadAccept;
import org.avuna.httpd.http.networking.ThreadConnection;
import org.avuna.httpd.http.networking.ThreadWorker;

public class HostHTTP extends Host {
	
	private ArrayList<VHost> vhosts = new ArrayList<VHost>();
	private int tac, tcc, twc, mc;
	
	public void addVHost(VHost vhost) {
		vhosts.add(vhost);
	}
	
	public HostHTTP(String name, String ip, int port, boolean isSSL, File keyFile, String keyPassword, String keystorePassword) {
		super(name, ip, port, isSSL, keyFile, keyPassword, keystorePassword, Protocol.HTTP);
	}
	
	public VHost getVHost(String host) {
		for (VHost vhost : vhosts) {
			if (vhost.getVHost().equals(".*") || host.matches(vhost.getVHost())) {
				return vhost;
			}
		}
		return null;
	}
	
	public VHost getVHostByName(String name) {
		for (VHost vhost : vhosts) {
			if (vhost.getName().equals(this.name + "/" + name)) {
				return vhost;
			}
		}
		return null;
	}
	
	public ArrayList<VHost> getVHosts() {
		return vhosts;
	}
	
	public void setupFolders() {
		for (VHost vhost : vhosts) {
			vhost.setupFolders();
		}
	}
	
	public void formatConfig(HashMap<String, Object> map) {
		if (!map.containsKey("errorpages")) map.put("errorpages", new LinkedHashMap<String, Object>());
		if (!map.containsKey("index")) map.put("index", "index.class,index.jwsl,index.php,index.html");
		if (!map.containsKey("cacheClock")) map.put("cacheClock", "-1");
		if (!map.containsKey("acceptThreadCount")) map.put("acceptThreadCount", "4");
		if (!map.containsKey("connThreadCount")) map.put("connThreadCount", "12");
		if (!map.containsKey("workerThreadCount")) map.put("workerThreadCount", "32");
		if (!map.containsKey("maxConnections")) map.put("maxConnections", "-1");
		tac = Integer.parseInt((String)map.get("acceptThreadCount"));
		tcc = Integer.parseInt((String)map.get("connThreadCount"));
		twc = Integer.parseInt((String)map.get("workerThreadCount"));
		mc = Integer.parseInt((String)map.get("maxConnections"));
		if (!map.containsKey("vhosts")) map.put("vhosts", new LinkedHashMap<String, Object>());
		HashMap<String, Object> vhosts = (HashMap<String, Object>)map.get("vhosts");
		if (!vhosts.containsKey("main")) vhosts.put("main", new LinkedHashMap<String, Object>());
		for (String vkey : vhosts.keySet()) {
			HashMap<String, Object> vhost = (HashMap<String, Object>)vhosts.get(vkey);
			if (!vhost.containsKey("enabled")) vhost.put("enabled", "true");
			if (!vhost.containsKey("debug")) vhost.put("debug", "false");
			if (!vhost.containsKey("host")) vhost.put("host", ".*");
			if (!vhost.containsKey("htdocs")) vhost.put("htdocs", AvunaHTTPD.fileManager.getBaseFile("htdocs").toString());
			if (!vhost.containsKey("htsrc")) vhost.put("htsrc", AvunaHTTPD.fileManager.getBaseFile("htsrc").toString());
		}
		for (String vkey : vhosts.keySet()) {
			HashMap<String, Object> ourvh = (HashMap<String, Object>)vhosts.get(vkey);
			if (!ourvh.get("enabled").equals("true")) continue;
			VHost vhost = new VHost(this.getHostname() + "/" + vkey, this, new File((String)ourvh.get("htdocs")), new File((String)ourvh.get("htsrc")), (String)ourvh.get("host"));
			vhost.setDebug(ourvh.get("debug").equals("true"));
			this.addVHost(vhost);
		}
	}
	
	public void setup(ServerSocket s) {
		ThreadConnection.initQueue(mc < 1 ? 10000000 : mc);
		ThreadWorker.initQueue();
		for (int i = 0; i < twc; i++) {
			new ThreadWorker().start();
		}
		for (int i = 0; i < tcc; i++) {
			new ThreadConnection().start();
		}
		for (int i = 0; i < tac; i++) {
			new ThreadAccept(this, s, mc).start();
		}
	}
}
