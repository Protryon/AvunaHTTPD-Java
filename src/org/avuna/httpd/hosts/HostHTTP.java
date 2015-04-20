package org.avuna.httpd.hosts;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.Logger;

public class HostHTTP extends Host {
	
	private ArrayList<VHost> vhosts = new ArrayList<VHost>();
	protected int tac, tcc;
	protected int twc;
	protected int mc;
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
	
	protected HostHTTP(String name, Protocol protocol) {
		super(name, protocol);
		this.registry = new PatchRegistry(this);
		patchBus = new PatchBus(registry);
	}
	
	public void loadBases() {
		Logger.log("Loading Base Plugins for " + name);
		BaseLoader.loadBases(registry);
	}
	
	public void loadCustoms() {
		Logger.log("Loading Custom Plugins for " + name);
		BaseLoader.loadCustoms(registry, new File(getConfig().getNode("plugins").getValue()));
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
		new File(getConfig().getNode("plugins").getValue()).mkdirs();
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
	
	public void formatConfig(ConfigNode map) {
		formatConfig(map, true);
	}
	
	public void formatConfig(ConfigNode map, boolean loadVHosts) {
		super.formatConfig(map);
		if (!map.containsNode("errorpages")) map.insertNode("errorpages");
		if (!map.containsNode("index")) map.insertNode("index", "index.class,index.php,index.html");
		if (!map.containsNode("cacheClock")) map.insertNode("cacheClock", "-1");
		if (!map.containsNode("acceptThreadCount")) map.insertNode("acceptThreadCount", "4");
		if (!map.containsNode("connThreadCount")) map.insertNode("connThreadCount", "12");
		if (!map.containsNode("workerThreadCount")) map.insertNode("workerThreadCount", "32");
		if (!map.containsNode("maxConnections")) map.insertNode("maxConnections", "-1");
		if (!map.containsNode("http2")) map.insertNode("http2", "false");
		if (!map.containsNode("plugins")) map.insertNode("plugins", AvunaHTTPD.fileManager.getBaseFile("plugins").toString());
		tac = Integer.parseInt(map.getNode("acceptThreadCount").getValue());
		tcc = Integer.parseInt(map.getNode("connThreadCount").getValue());
		twc = Integer.parseInt(map.getNode("workerThreadCount").getValue());
		mc = Integer.parseInt(map.getNode("maxConnections").getValue());
		http2 = map.getNode("http2").getValue().equals("true");
		if (!map.containsNode("vhosts")) map.insertNode("vhosts");
		ConfigNode vhosts = map.getNode("vhosts");
		if (!vhosts.containsNode("main")) vhosts.insertNode("main");
		for (String vkey : vhosts.getSubnodes()) {
			ConfigNode vhost = vhosts.getNode(vkey);
			if (!vhost.containsNode("enabled")) vhost.insertNode("enabled", "true");
			if (!vhost.containsNode("debug")) vhost.insertNode("debug", "false");
			if (!vhost.containsNode("host")) vhost.insertNode("host", ".*");
			if (!vhost.containsNode("inheritjls")) {
				if (!vhost.containsNode("htdocs")) vhost.insertNode("htdocs", AvunaHTTPD.fileManager.getBaseFile("htdocs").toString());
				if (!vhost.containsNode("htsrc")) vhost.insertNode("htsrc", AvunaHTTPD.fileManager.getBaseFile("htsrc").toString());
			}
		}
		if (loadVHosts) {
			for (String vkey : vhosts.getSubnodes()) {
				ConfigNode ourvh = vhosts.getNode(vkey);
				if (!ourvh.getNode("enabled").getValue().equals("true")) continue;
				VHost vhost = null;
				if (ourvh.containsNode("inheritjls")) {
					VHost parent = null;
					String ij = ourvh.getNode("inheritjls").getValue();
					for (Host host : AvunaHTTPD.hosts.values()) {
						if (host.name.equals(ij.substring(0, ij.indexOf("/")))) {
							parent = ((HostHTTP)host).getVHostByName(ij.substring(ij.indexOf("/") + 1));
						}
					}
					if (parent == null) {
						Logger.log("Invalid inheritjls! Skipping");
						continue;
					}
					vhost = new VHost(this.getHostname() + "/" + vkey, this, ourvh.getNode("host").getValue(), parent);
				}else {
					vhost = new VHost(this.getHostname() + "/" + vkey, this, new File(ourvh.getNode("htdocs").getValue()), new File(ourvh.getNode("htsrc").getValue()), ourvh.getNode("host").getValue());
				}
				vhost.setDebug(ourvh.getNode("debug").getValue().equals("true"));
				this.addVHost(vhost);
			}
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
