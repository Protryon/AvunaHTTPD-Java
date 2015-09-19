/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.hosts;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.http.plugins.Plugin;
import org.avuna.httpd.http.plugins.PluginRegistry;
import org.avuna.httpd.http.plugins.avunaagent.AvunaAgentSession;
import org.avuna.httpd.http.plugins.avunaagent.PluginAvunaAgent;
import org.avuna.httpd.http.plugins.base.BaseLoader;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.logging.Logger;
import org.avuna.httpd.util.unio.Certificate;

public class VHost {
	private final HostHTTP host;
	private final File htdocs, htsrc, htcfg;
	private final String name, vhost;
	private AvunaAgentSession jls;
	private final VHost parent;
	private final int cacheClock;
	private final String[] index;
	private final ConfigNode errorpages;
	private final boolean forward;
	private final boolean unix;
	private final String ip;
	private final int port;
	private final File plugins;
	public final PluginRegistry registry;
	public final Logger logger;
	public final EventBus eventBus = new EventBus();
	public Certificate sniCert;
	
	public boolean isForwarding() {
		return forward;
	}
	
	public void destroy() {
		for (Plugin pl : registry.patchs) {
			pl.destroy();
			host.eventBus.removeListener(pl);
		}
	}
	
	public boolean isForwardUnix() {
		return unix;
	}
	
	public String getForwardIP() {
		return ip;
	}
	
	public int getForwardPort() {
		return port;
	}
	
	private ArrayList<VHost> children = new ArrayList<VHost>();
	
	public VHost(String name, HostHTTP host, String vhost, VHost parent, int cacheClock, String index, ConfigNode errorpages, boolean forward, boolean unix, String ip, int port, File plugins) {
		this.name = name;
		this.host = host;
		this.htdocs = parent.htdocs;
		this.htsrc = parent.htsrc;
		this.htcfg = parent.htcfg;
		this.vhost = vhost;
		this.parent = parent;
		this.cacheClock = cacheClock;
		this.index = index.split(",");
		this.errorpages = errorpages;
		this.forward = forward;
		this.unix = unix;
		this.ip = ip;
		this.port = port;
		this.plugins = plugins;
		this.logger = new Logger(this);
		registry = new PluginRegistry(this);
		parent.children.add(this);
	}
	
	public VHost(String name, HostHTTP host, File htdocs, File htsrc, File htcfg, String vhost, int cacheClock, String index, ConfigNode errorpages, boolean forward, boolean unix, String ip, int port, File plugins) {
		this.name = name;
		this.host = host;
		this.htdocs = htdocs;
		this.htsrc = htsrc;
		this.htcfg = htcfg;
		this.vhost = vhost;
		this.parent = null;
		this.cacheClock = cacheClock;
		this.index = index == null ? null : index.split(",");
		this.errorpages = errorpages;
		this.forward = forward;
		this.unix = unix;
		this.ip = ip;
		this.port = port;
		this.plugins = plugins;
		this.logger = new Logger(this);
		registry = new PluginRegistry(this);
	}
	
	public void loadBases() {
		logger.log("Loading Base Plugins for " + name);
		BaseLoader.loadBases(registry);
	}
	
	public void loadCustoms() {
		logger.log("Loading Custom Plugin Configs for " + name);
		BaseLoader.loadCustoms(host, registry, plugins);
	}
	
	public File getPlugins() {
		return plugins;
	}
	
	public int getCacheClock() {
		return cacheClock;
	}
	
	public String[] getIndex() {
		return index;
	}
	
	public ConfigNode getErrorPages() {
		return errorpages;
	}
	
	public boolean isChild() {
		return parent != null;
	}
	
	public void initJLS(PluginAvunaAgent pll, URL[] url) {
		if (this.parent == null) {
			this.jls = new AvunaAgentSession(pll, this, url);
			for (VHost child : children) {
				child.jls = this.jls;
			}
		}else while (this.jls == null) {
			try {
				Thread.sleep(1L); // wait
			}catch (InterruptedException e) {
				host.logger.logError(e);
			}
		}
	}
	
	public String getName() {
		return name;
	}
	
	private boolean debug = false;
	
	public void setDebug(boolean set) {
		debug = set;
	}
	
	public boolean getDebug() {
		return debug;
	}
	
	public AvunaAgentSession getJLS() {
		return jls;
	}
	
	public String getVHost() {
		return vhost;
	}
	
	public HostHTTP getHost() {
		return host;
	}
	
	public void setupFolders() {
		if (htdocs != null) htdocs.mkdirs();
		if (htsrc != null) htsrc.mkdirs();
		if (htcfg != null) htcfg.mkdirs();
		if (plugins != null) plugins.mkdirs();
		for (Plugin patch : registry.patchs) {
			patch.config.mkdirs();
		}
	}
	
	public File getHTDocs() {
		return htdocs;
	}
	
	public File getHTSrc() {
		return htsrc;
	}
	
	public File getHTCfg() {
		return htcfg;
	}
	
	public String getHostPath() {
		return name;
	}
	
	public void setSNI(Certificate certificate) {
		this.sniCert = certificate;
	}
}
