package org.avuna.httpd.http.plugins;

import java.util.ArrayList;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.util.Logger;

public class PluginRegistry {
	public final HostHTTP host;
	
	public PluginRegistry(HostHTTP host) {
		this.host = host;
	}
	
	public void registerPatch(Plugin p) {
		if (!p.pcfg.containsNode("enabled") || !p.pcfg.getNode("enabled").getValue().equals("true")) return;
		Logger.log("Loading patch " + p.name);
		p.register(host.eventBus);
		patchs.add(p);
	}
	
	protected final ArrayList<Plugin> patchs = new ArrayList<Plugin>();
	
	public Plugin getPatchForClass(Class<?> cls) {
		for (Plugin p : patchs) {
			if (cls.isAssignableFrom(p.getClass())) {
				return p;
			}
		}
		return null;
	}
}
