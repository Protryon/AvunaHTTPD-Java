package org.avuna.httpd.http.plugins;

import org.avuna.httpd.AvunaHTTPD;

public class PluginBus {
	private final PluginRegistry registry;
	
	public PluginBus(PluginRegistry registry) {
		this.registry = registry;
	}
	
	public void setupFolders() {
		for (Plugin patch : registry.patchs) {
			if (patch.pcfg.getNode("enabled").getValue().equals("true")) {
				AvunaHTTPD.fileManager.getPlugin(patch).mkdirs();
			}
		}
	}
	
}
