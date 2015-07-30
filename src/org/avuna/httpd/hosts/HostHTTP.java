/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.hosts;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.event.Event;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.event.base.EventConnected;
import org.avuna.httpd.event.base.EventPostInit;
import org.avuna.httpd.event.base.EventPreExit;
import org.avuna.httpd.event.base.EventReload;
import org.avuna.httpd.event.base.EventSetupFolders;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.networking.ThreadAccept;
import org.avuna.httpd.http.networking.ThreadConnection;
import org.avuna.httpd.http.networking.ThreadWorker;
import org.avuna.httpd.http.networking.Work;
import org.avuna.httpd.http.plugins.PluginBus;
import org.avuna.httpd.http.plugins.PluginRegistry;
import org.avuna.httpd.http.plugins.base.BaseLoader;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.Logger;

public class HostHTTP extends Host {
	
	private ArrayList<VHost> vhosts = new ArrayList<VHost>();
	protected int tac, tcc;
	protected int twc;
	protected int mc;
	public final PluginRegistry registry;
	public final PluginBus patchBus;
	private int maxPostSize = 65535;
	
	public void postload() throws IOException {
		eventBus.callEvent(new EventPostInit());
	}
	
	public int getMaxPostSize() {
		return maxPostSize;
	}
	
	public void receive(EventBus bus, Event event) {
		if (event instanceof EventReload) {
			vhosts.clear();
			formatConfig(getConfig());
		}else {
			super.receive(bus, event);
		}
	}
	
	public void addVHost(VHost vhost) {
		vhosts.add(vhost);
	}
	
	public HostHTTP(String name) {
		super(name, Protocol.HTTP);
		this.registry = new PluginRegistry(this);
		patchBus = new PluginBus(registry);
	}
	
	protected HostHTTP(String name, Protocol protocol) {
		super(name, protocol);
		this.registry = new PluginRegistry(this);
		patchBus = new PluginBus(registry);
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
	
	public RequestPacket pollReqQueue() {
		return reqWorkQueue.poll();
	}
	
	public boolean emptyReqQueue() {
		return reqWorkQueue.isEmpty();
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
	
	public void deleteVHostByName(String name) {
		for (int i = 0; i < vhosts.size(); i++) {
			if (vhosts.get(i).getName().equals(this.name + "/" + name)) {
				vhosts.remove(i);
				return;
			}
		}
	}
	
	public ArrayList<VHost> getVHosts() {
		return vhosts;
	}
	
	public static void unpack() {
		
	}
	
	public List<Work> works = Collections.synchronizedList(new ArrayList<Work>());
	
	public void readdWork(Work w) {
		w.inUse = false;
		works.add(w);
	}
	
	public Work getWork() {
		synchronized (works) {
			for (int i = 0; i < works.size(); i++) {
				Work work = works.get(i);
				if (work.inUse) continue;
				try {
					if (work.s.isClosed()) {
						work.close();
						i--;
						continue;
					}else {
						ResponsePacket lrp = work.outQueue.peek();
						if (work.in.available() > 0 || (lrp != null && lrp.done)) {
							work.inUse = true;
							return work;
						}else {
							if (work.ssl) {
								work.inUse = true;
								work.s.setSoTimeout(1);
								try {
									int sp = work.in.read();
									if (sp == -1) {
										work.close();
										i--;
										continue;
									}
									work.sslprep.write(sp);
								}catch (SocketTimeoutException e) {
									
								}finally {
									work.s.setSoTimeout(1000);
								}
								if (work.in.available() > 0) {
									return work;
								}else {
									work.inUse = false;
								}
							}
							if (!work.blockTimeout) {
								work.inUse = true;
								if (work.sns == 0L) {
									work.sns = System.nanoTime() + 10000000000L;
									work.inUse = false;
									continue;
								}else {
									if (work.sns >= System.nanoTime()) {
										if (AvunaHTTPD.bannedIPs.contains(work.s.getInetAddress().getHostAddress())) {
											work.close();
											i--;
										}
										work.inUse = false;
										continue;
									}else {
										work.close();
										i--;
										continue;
									}
								}
							}
						}
					}
				}catch (IOException e) {
					work.inUse = true;
					return work;
				}
			}
		}
		return null;
	}
	
	public final ArrayList<Thread> subworkers = new ArrayList<Thread>();
	
	public ArrayList<ThreadConnection> conns = new ArrayList<ThreadConnection>();
	public ArrayList<ThreadWorker> workers = new ArrayList<ThreadWorker>();
	public static HashMap<String, Integer> connIPs = new HashMap<String, Integer>();
	
	public static int getConnectionsForIP(String ip) {
		return connIPs.containsKey(ip) ? connIPs.get(ip) : 0;
	}
	
	public void addWork(HostHTTP host, Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
		String ip = s.getInetAddress().getHostAddress();
		Integer cur = connIPs.get(ip);
		if (cur == null) cur = 0;
		cur += 1;
		connIPs.put(ip, cur);
		Work w = new Work(host, s, in, out, ssl);
		Logger.log(ip + " connected to " + host.getHostname() + ".");
		EventConnected epc = new EventConnected(w);
		host.eventBus.callEvent(epc);
		if (epc.isCanceled()) {
			w.close();
			return;
		}
		works.add(w);
		
	}
	
	public void clearIPs(String ip) {
		synchronized (works) {
			for (int i = 0; i < works.size(); i++) {
				Work work = works.get(i);
				if (work.s.getInetAddress().getHostAddress().equals(ip)) {
					works.remove(i--);
				}
			}
		}
	}
	
	public void removeWork(Work tr) {
		works.remove(tr);
	}
	
	public void preExit() {
		eventBus.callEvent(new EventPreExit());
	}
	
	public void setupFolders() {
		for (VHost vhost : vhosts) {
			vhost.setupFolders();
		}
		new File(getConfig().getNode("plugins").getValue()).mkdirs();
		eventBus.callEvent(new EventSetupFolders());
		patchBus.setupFolders();
	}
	
	public void clearReqWork() {
		reqWorkQueue.clear();
	}
	
	private ArrayBlockingQueue<RequestPacket> reqWorkQueue;
	
	public void addWork(RequestPacket req) {
		reqWorkQueue.add(req);
		for (ThreadWorker worker : workers) {
			if (worker.getState() == State.WAITING) {
				synchronized (worker) {
					worker.notify();
				}
				break;
			}
		}
	}
	
	public void initQueue() {
		reqWorkQueue = new ArrayBlockingQueue<RequestPacket>(1000000);
	}
	
	public ResponsePacket[] processSubRequests(RequestPacket... reqs) {
		ResponsePacket[] resps = new ResponsePacket[reqs.length];
		for (int i = 0; i < resps.length; i++) {
			if (reqs[i] == null) continue;
			resps[i] = new ResponsePacket();
			reqs[i].child = resps[i];
			resps[i].request = reqs[i];
		}
		for (int i = 0; i < resps.length; i++) {
			if (reqs[i] == null) continue;
			addWork(reqs[i]);
		}
		major: while (true) {
			for (ResponsePacket resp : resps) {
				if (resp != null && !resp.done) {
					try {
						Thread.sleep(0L, 400000); // TODO: longer? smarter?
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
		if (!map.containsNode("maxPostSize")) map.insertNode("maxPostSize", "65535", "max post size in KB");
		this.maxPostSize = Integer.parseInt(map.getNode("maxPostSize").getValue());
		if (!map.containsNode("acceptThreadCount")) map.insertNode("acceptThreadCount", "4", "number of accept threads");
		if (!map.containsNode("connThreadCount")) map.insertNode("connThreadCount", "12", "number of connection threads");
		if (!map.containsNode("workerThreadCount")) map.insertNode("workerThreadCount", "32", "number of HTTP worker threads");
		if (!map.containsNode("maxConnections")) map.insertNode("maxConnections", "-1", "-1 for infinite, >0 for a maximum amount.");
		if (!map.containsNode("http2")) map.insertNode("http2", "false", "not fully implemented, reccomended against use");
		if (!map.containsNode("plugins")) map.insertNode("plugins", AvunaHTTPD.fileManager.getBaseFile("plugins").toString());
		tac = Integer.parseInt(map.getNode("acceptThreadCount").getValue());
		tcc = Integer.parseInt(map.getNode("connThreadCount").getValue());
		twc = Integer.parseInt(map.getNode("workerThreadCount").getValue());
		mc = Integer.parseInt(map.getNode("maxConnections").getValue());
		http2 = map.getNode("http2").getValue().equals("true");
		if (!map.containsNode("vhosts")) map.insertNode("vhosts", null, "host values are checked in the order of this file");
		ConfigNode vhosts = map.getNode("vhosts");
		if (!vhosts.containsNode("main")) vhosts.insertNode("main", "main node must exist, since it is a catch all, I reccomend you keep it at the bottom for the above reason");
		for (String vkey : vhosts.getSubnodes()) {
			ConfigNode vhost = vhosts.getNode(vkey);
			if (!vhost.containsNode("enabled")) vhost.insertNode("enabled", "true");
			if (!vhost.containsNode("debug")) vhost.insertNode("debug", "false", "if true, request headers will be logged");
			if (!vhost.containsNode("host")) vhost.insertNode("host", ".*", "regex to match host header, determines which vhost to load.");
			// if (!vhost.containsNode("inheritjls")) {
			if (!vhost.containsNode("htdocs")) vhost.insertNode("htdocs", AvunaHTTPD.fileManager.getBaseFile("htdocs").toString());
			if (!vhost.containsNode("htsrc")) vhost.insertNode("htsrc", AvunaHTTPD.fileManager.getBaseFile("htsrc").toString());
			// }
			if (!vhost.containsNode("inheritjls")) vhost.insertNode("inheritjls", "", "set to host/vhost to inherit another hosts javaloaders; used for HTTPS, you would want to inherit JLS to sync them in memory.");
			if (!vhost.containsNode("errorpages")) vhost.insertNode("errorpages", null, "subvalues are errorcode=errorpage, ie \"404=/404.html\", does not support PHP/AvunaAgent pages.");
			if (!vhost.containsNode("index")) vhost.insertNode("index", "index.class,index.php,index.html", "format is filename,filename,etc");
			if (!vhost.containsNode("cacheClock")) vhost.insertNode("cacheClock", "-1", "-1=forever, 0=never >0=MS per cache clear");
		}
		if (loadVHosts) {
			for (String vkey : vhosts.getSubnodes()) {
				ConfigNode ourvh = vhosts.getNode(vkey);
				if (!ourvh.getNode("enabled").getValue().equals("true")) continue;
				VHost vhost = null;
				if (ourvh.containsNode("inheritjls") && ourvh.getNode("inheritjls").getValue().length() > 0) {
					VHost parent = null;
					String ij = ourvh.getNode("inheritjls").getValue();
					for (Host host : AvunaHTTPD.hosts.values()) {
						if (host.name.equals(ij.substring(0, ij.indexOf("/")))) {
							parent = ((HostHTTP) host).getVHostByName(ij.substring(ij.indexOf("/") + 1));
						}
					}
					if (parent == null) {
						Logger.log("Invalid inheritjls! Skipping.");
						continue;
					}
					vhost = new VHost(this.getHostname() + "/" + vkey, this, ourvh.getNode("host").getValue(), parent, Integer.parseInt(ourvh.getNode("cacheClock").getValue()), ourvh.getNode("index").getValue(), ourvh.getNode("errorpages"));
				}else {
					vhost = new VHost(this.getHostname() + "/" + vkey, this, new File(ourvh.getNode("htdocs").getValue()), new File(ourvh.getNode("htsrc").getValue()), ourvh.getNode("host").getValue(), Integer.parseInt(ourvh.getNode("cacheClock").getValue()), ourvh.getNode("index").getValue(), ourvh.getNode("errorpages"));
				}
				vhost.setDebug(ourvh.getNode("debug").getValue().equals("true"));
				this.addVHost(vhost);
			}
		}
	}
	
	public void setup(ServerSocket s) {
		// initQueue(mc < 1 ? 10000000 : mc);
		initQueue();
		for (int i = 0; i < twc; i++) {
			ThreadWorker tw = new ThreadWorker(this);
			addTerm(tw);
			workers.add(tw);
			tw.start();
		}
		for (int i = 0; i < tcc; i++) {
			ThreadConnection tc = new ThreadConnection(this);
			try {
				Thread.sleep(0L, 500000);
			}catch (InterruptedException e) {}
			addTerm(tc);
			tc.start();
		}
		for (int i = 0; i < tac; i++) {
			ThreadAccept ta = new ThreadAccept(this, s, mc);
			ta.start();
		}
	}
}
