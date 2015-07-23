/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

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
			if (!vhost.containsNode("debug")) vhost.insertNode("debug", "false", "if true, request headers will be logged");
			if (!vhost.containsNode("host")) vhost.insertNode("host", ".*", "regex to match host header, determines which vhost to load.");
			vhost.removeNode("inheritjls");
			vhost.removeNode("htdocs");
			vhost.removeNode("htsrc");
			// if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
			// if (!vhost.containsNode("spawn")) vhost.insertNode("spawn", "true");
			// if (!vhost.containsNode("uid")) vhost.insertNode(AvunaHTTPD.mainConfig.get("uid"));
			// if (!vhost.containsNode("gid")) vhost.insertNode(AvunaHTTPD.mainConfig.get("gid"));
			// }
			if (!AvunaHTTPD.windows && !vhost.containsNode("unix")) vhost.insertNode("unix", "false", "enabled unix socket. if true, set the ip to the unix socket file, port is ignored.");
			if (!vhost.containsNode("ip")) vhost.insertNode("ip", "127.0.0.1", "ip or unix socket file to forward to");
			if (!vhost.containsNode("port")) vhost.insertNode("port", "6844", "port to forward if ip");
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
		// initQueue(mc < 1 ? 10000000 : mc);
		initQueue();
		for (int i = 0; i < twc; i++) {
			ThreadMWorker tmw = new ThreadMWorker(this);
			addTerm(tmw);
			tmw.start();
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
			new ThreadAccept(this, s, mc).start();
		}
	}
}
