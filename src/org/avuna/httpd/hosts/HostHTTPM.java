package org.avuna.httpd.hosts;

import java.net.ServerSocket;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.http.networking.ThreadAccept;
import org.avuna.httpd.http.networking.ThreadConnection;
import org.avuna.httpd.http.networking.httpm.ThreadMWorker;
import org.avuna.httpd.http.plugins.base.BaseLoader;
import org.avuna.httpd.util.ConfigNode;
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
	
	public void formatConfig(ConfigNode map) {
		super.formatConfig(map, false);
		map.removeNode("index");
		map.removeNode("cacheClock");
		ConfigNode vhosts = map.getNode("vhosts");
		for (String vkey : vhosts.getSubnodes()) {
			ConfigNode vhost = vhosts.getNode(vkey);
			if (!vhost.containsNode("enabled")) vhost.insertNode("enabled", "true");
			if (!vhost.containsNode("debug")) vhost.insertNode("debug", "false");
			if (!vhost.containsNode("host")) vhost.insertNode("host", ".*");
			vhost.removeNode("inheritjls");
			vhost.removeNode("htdocs");
			vhost.removeNode("htsrc");
			// if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
			// if (!vhost.containsNode("spawn")) vhost.insertNode("spawn", "true");
			// if (!vhost.containsNode("uid")) vhost.insertNode(AvunaHTTPD.mainConfig.get("uid"));
			// if (!vhost.containsNode("gid")) vhost.insertNode(AvunaHTTPD.mainConfig.get("gid"));
			// }
			if (!AvunaHTTPD.windows && !vhost.containsNode("unix")) vhost.insertNode("unix", "false", "enabled unix socket. if true, set the ip to the unix socket file, port is ignored.");
			if (!vhost.containsNode("ip")) vhost.insertNode("ip", "127.0.0.1");
			if (!vhost.containsNode("port")) vhost.insertNode("port", "6844");
		}
		for (String vkey : vhosts.getSubnodes()) {
			ConfigNode ourvh = vhosts.getNode(vkey);
			if (!ourvh.getNode("enabled").getValue().equals("true")) continue;
			boolean unix = !AvunaHTTPD.windows && ourvh.getNode("unix").getValue().equals("true");
			VHost vhost = new VHostM(this.getHostname() + "/" + vkey, this, unix, ourvh.getNode("host").getValue(), ourvh.getNode("ip").getValue(), Integer.parseInt(ourvh.getNode("port").getValue()));
			vhost.setDebug(ourvh.getNode("debug").getValue().equals("true"));
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
