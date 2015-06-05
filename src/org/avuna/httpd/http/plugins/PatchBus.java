package org.avuna.httpd.http.plugins;

import org.avuna.httpd.AvunaHTTPD;

public class PatchBus {
	private final PatchRegistry registry;
	
	public PatchBus(PatchRegistry registry) {
		this.registry = registry;
	}
	
	public void setupFolders() {
		for (Patch patch : registry.patchs) {
			if (patch.pcfg.getNode("enabled").getValue().equals("true")) {
				AvunaHTTPD.fileManager.getPlugin(patch).mkdirs();
			}
		}
	}
	
}
