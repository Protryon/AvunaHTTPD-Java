package org.avuna.httpd.hosts;

import java.net.ServerSocket;
import java.util.HashMap;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.http.networking.ThreadAccept;
import org.avuna.httpd.http.networking.ThreadConnection;
import org.avuna.httpd.http.networking.httpm.ThreadMWorker;
import org.avuna.httpd.http.plugins.base.BaseLoader;
import org.avuna.httpd.util.Logger;

public class HostHTTPM extends HostHTTP {
	
	public HostHTTPM(String name) {
		super(name, Protocol.HTTPM);
	}
	
	public void loadBases() {
		Logger.log("Loading HTTPM Security");
		BaseLoader.loadSecBase(registry);
	}
	
	public void loadCustoms() {
		
	}
	
	public void formatConfig(HashMap<String, Object> map) {
		super.formatConfig(map, false);
		HashMap<String, Object> vhosts = (HashMap<String, Object>)map.get("vhosts");
		for (String vkey : vhosts.keySet()) {
			HashMap<String, Object> vhost = (HashMap<String, Object>)vhosts.get(vkey);
			if (!vhost.containsKey("enabled")) vhost.put("enabled", "true");
			if (!vhost.containsKey("debug")) vhost.put("debug", "false");
			if (!vhost.containsKey("host")) vhost.put("host", ".*");
			vhost.remove("inheritjls");
			vhost.remove("htdocs");
			vhost.remove("htsrc");
			if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
				if (!vhost.containsKey("spawn")) vhost.put("spawn", "true");
				if (!vhost.containsKey("uid")) vhost.put("uid", AvunaHTTPD.mainConfig.get("uid"));
				if (!vhost.containsKey("gid")) vhost.put("gid", AvunaHTTPD.mainConfig.get("gid"));
			}
			if (!vhost.containsKey("vhost-ip")) vhost.put("vhost-ip", "127.0.0.1");
			if (!vhost.containsKey("vhost-port")) vhost.put("vhost-port", "6844");
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
						parent = ((HostHTTPM)host).getVHostByName(ij.substring(ij.indexOf("/") + 1));
					}
				}
				if (parent == null) {
					Logger.log("Invalid inheritjls! Skipping");
					continue;
				}
				vhost = new VHostM(this.getHostname() + "/" + vkey, this, (String)ourvh.get("host"), parent, (String)ourvh.get("vhost-ip"), Integer.parseInt((String)ourvh.get("vhost-port")));
			}else {
				vhost = new VHostM(this.getHostname() + "/" + vkey, this, (String)ourvh.get("host"), (String)ourvh.get("vhost-ip"), Integer.parseInt((String)ourvh.get("vhost-port")));
			}
			vhost.setDebug(ourvh.get("debug").equals("true"));
			this.addVHost(vhost);
		}
	}
	
	public void setup(ServerSocket s) {
		initQueue(mc < 1 ? 10000000 : mc);
		initQueue();
		for (int i = 0; i < twc; i++) {
			new ThreadMWorker(this).start();
		}
		for (int i = 0; i < tcc; i++) {
			new ThreadConnection(this).start();
		}
		for (int i = 0; i < tac; i++) {
			new ThreadAccept(this, s, mc).start();
		}
	}
}
