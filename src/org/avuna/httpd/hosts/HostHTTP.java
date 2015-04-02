package org.avuna.httpd.hosts;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.networking.ThreadAccept;
import org.avuna.httpd.http.networking.ThreadConnection;
import org.avuna.httpd.http.networking.ThreadWorker;
import org.avuna.httpd.http.networking.Work;
import org.avuna.httpd.http.plugins.PatchBus;
import org.avuna.httpd.http.plugins.PatchRegistry;
import org.avuna.httpd.http.plugins.base.BaseLoader;
import org.avuna.httpd.util.Logger;

public class HostHTTP extends Host {
	
	private ArrayList<VHost> vhosts = new ArrayList<VHost>();
	private int tac, tcc, twc, mc;
	public final PatchRegistry registry;
	public final PatchBus patchBus;
	
	public void addVHost(VHost vhost) {
		vhosts.add(vhost);
	}
	
	public HostHTTP(String name) {
		super(name, Protocol.HTTP);
		this.registry = new PatchRegistry(this);
		patchBus = new PatchBus(registry);
	}
	
	public void loadBases() {
		Logger.log("Loading Base Plugins for " + name);
		BaseLoader.loadBases(registry);
	}
	
	public void loadCustoms() {
		Logger.log("Loading Custom Plugins for " + name);
		BaseLoader.loadCustoms(registry, new File((String)getConfig().get("plugins")));
	}
	
	public VHost getVHost(String host) {
		for (VHost vhost : vhosts) {
			if (vhost.getVHost().equals(".*") || host.matches(vhost.getVHost())) {
				return vhost;
			}
		}
		return null;
	}
	
	public Work pollQueue() {
		return workQueue.poll();
	}
	
	public boolean emptyQueue() {
		return workQueue.isEmpty();
	}
	
	public RequestPacket pollReqQueue() {
		return reqWorkQueue.poll();
	}
	
	public boolean emptyReqQueue() {
		return reqWorkQueue.isEmpty();
	}
	
	public int sizeQueue() {
		return workQueue.size();
	}
	
	public int sizeReqQueue() {
		return reqWorkQueue.size();
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
	
	public static void unpack() {
		
	}
	
	public void clearWork() {
		workQueue.clear();
	}
	
	public int getQueueSize() {
		return workQueue.size();
	}
	
	public final ArrayList<Thread> subworkers = new ArrayList<Thread>();
	
	public ArrayList<ThreadConnection> conns = new ArrayList<ThreadConnection>();
	private ArrayBlockingQueue<Work> workQueue;
	public static HashMap<String, Integer> connIPs = new HashMap<String, Integer>();
	
	public void initQueue(int connlimit) {
		workQueue = new ArrayBlockingQueue<Work>(connlimit);
	}
	
	public static int getConnectionsForIP(String ip) {
		return connIPs.containsKey(ip) ? connIPs.get(ip) : 0;
	}
	
	public void addWork(HostHTTP host, Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
		String ip = s.getInetAddress().getHostAddress();
		Integer cur = connIPs.get(ip);
		if (cur == null) cur = 0;
		cur += 1;
		connIPs.put(ip, cur);
		workQueue.add(new Work(host, s, in, out, ssl));
		Logger.log(ip + " connected to " + host.getHostname() + ".");
	}
	
	public void clearIPs(String ip) {
		for (Object worko : workQueue.toArray()) {
			Work work = (Work)worko;
			if (work.s.getInetAddress().getHostAddress().equals(ip)) {
				workQueue.remove(work);
			}
		}
	}
	
	public void readdWork(Work work) {
		workQueue.add(work);
	}
	
	public void preExit() {
		patchBus.preExit();
	}
	
	public void setupFolders() {
		for (VHost vhost : vhosts) {
			vhost.setupFolders();
		}
		new File((String)getConfig().get("plugins")).mkdirs();
		patchBus.setupFolders();
	}
	
	public void clearReqWork() {
		reqWorkQueue.clear();
	}
	
	public ArrayList<ThreadWorker> workers = new ArrayList<ThreadWorker>();
	private ArrayBlockingQueue<RequestPacket> reqWorkQueue;
	
	public void addWork(RequestPacket req) {
		reqWorkQueue.add(req);
	}
	
	public void initQueue() {
		reqWorkQueue = new ArrayBlockingQueue<RequestPacket>(1000000);
	}
	
	public ResponsePacket[] processSubRequests(RequestPacket... reqs) {
		ResponsePacket[] resps = new ResponsePacket[reqs.length];
		for (int i = 0; i < resps.length; i++) {
			resps[i] = new ResponsePacket();
			reqs[i].child = resps[i];
			resps[i].request = reqs[i];
		}
		for (int i = 0; i < resps.length; i++) {
			addWork(reqs[i]);
		}
		major:
		while (true) {
			for (ResponsePacket resp : resps) {
				if (!resp.done) {
					try {
						Thread.sleep(0L, 100000); // TODO: longer? smarter?
					}catch (InterruptedException e) {
						Logger.logError(e);
					}
					continue major;
				}
			}
			break;
		}
		return resps;
	}
	
	public boolean http2 = false;
	
	public void formatConfig(HashMap<String, Object> map) {
		super.formatConfig(map);
		if (!map.containsKey("errorpages")) map.put("errorpages", new LinkedHashMap<String, Object>());
		if (!map.containsKey("index")) map.put("index", "index.class,index.php,index.html");
		if (!map.containsKey("cacheClock")) map.put("cacheClock", "-1");
		if (!map.containsKey("acceptThreadCount")) map.put("acceptThreadCount", "4");
		if (!map.containsKey("connThreadCount")) map.put("connThreadCount", "12");
		if (!map.containsKey("workerThreadCount")) map.put("workerThreadCount", "32");
		if (!map.containsKey("maxConnections")) map.put("maxConnections", "-1");
		if (!map.containsKey("http2")) map.put("http2", "false");
		if (!map.containsKey("plugins")) map.put("plugins", AvunaHTTPD.fileManager.getBaseFile("plugins").toString());
		tac = Integer.parseInt((String)map.get("acceptThreadCount"));
		tcc = Integer.parseInt((String)map.get("connThreadCount"));
		twc = Integer.parseInt((String)map.get("workerThreadCount"));
		mc = Integer.parseInt((String)map.get("maxConnections"));
		http2 = map.get("http2").equals("true");
		if (!map.containsKey("vhosts")) map.put("vhosts", new LinkedHashMap<String, Object>());
		HashMap<String, Object> vhosts = (HashMap<String, Object>)map.get("vhosts");
		if (!vhosts.containsKey("main")) vhosts.put("main", new LinkedHashMap<String, Object>());
		for (String vkey : vhosts.keySet()) {
			HashMap<String, Object> vhost = (HashMap<String, Object>)vhosts.get(vkey);
			if (!vhost.containsKey("enabled")) vhost.put("enabled", "true");
			if (!vhost.containsKey("debug")) vhost.put("debug", "false");
			if (!vhost.containsKey("host")) vhost.put("host", ".*");
			if (!vhost.containsKey("inheritjls")) {
				if (!vhost.containsKey("htdocs")) vhost.put("htdocs", AvunaHTTPD.fileManager.getBaseFile("htdocs").toString());
				if (!vhost.containsKey("htsrc")) vhost.put("htsrc", AvunaHTTPD.fileManager.getBaseFile("htsrc").toString());
			}
		}
		for (String vkey : vhosts.keySet()) {
			HashMap<String, Object> ourvh = (HashMap<String, Object>)vhosts.get(vkey);
			if (!ourvh.get("enabled").equals("true")) continue;
			VHost vhost = null;
			if (ourvh.containsKey("inheritjls")) {
				VHost parent = null;
				String ij = (String)ourvh.get("inheritjls");
				for (Host host : AvunaHTTPD.hosts.values()) {
					if (host.name.equals(ij.substring(0, ij.indexOf("/")))) {
						parent = ((HostHTTP)host).getVHostByName(ij.substring(ij.indexOf("/") + 1));
					}
				}
				if (parent == null) {
					Logger.log("Invalid inheritjls! Skipping");
					continue;
				}
				vhost = new VHost(this.getHostname() + "/" + vkey, this, (String)ourvh.get("host"), parent);
			}else {
				vhost = new VHost(this.getHostname() + "/" + vkey, this, new File((String)ourvh.get("htdocs")), new File((String)ourvh.get("htsrc")), (String)ourvh.get("host"));
			}
			vhost.setDebug(ourvh.get("debug").equals("true"));
			this.addVHost(vhost);
		}
	}
	
	public void setup(ServerSocket s) {
		initQueue(mc < 1 ? 10000000 : mc);
		initQueue();
		for (int i = 0; i < twc; i++) {
			new ThreadWorker(this).start();
		}
		for (int i = 0; i < tcc; i++) {
			new ThreadConnection(this).start();
		}
		for (int i = 0; i < tac; i++) {
			new ThreadAccept(this, s, mc).start();
		}
	}
}
