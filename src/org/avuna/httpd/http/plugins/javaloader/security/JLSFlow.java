package org.avuna.httpd.http.plugins.javaloader.security;

import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.plugins.base.PluginInline;
import org.avuna.httpd.http.plugins.javaloader.JavaLoaderSecurity;
import org.avuna.httpd.util.Logger;

public class JLSFlow extends JavaLoaderSecurity {
	
	@SuppressWarnings("unused")
	private int returnWeight = 0;
	private boolean enabled = true;
	
	public void init() {
		if (!pcfg.containsNode("returnWeight")) pcfg.insertNode("returnWeight", "25").setComment("For each descrepancy, ex. a logo is not loaded.");
		if (!pcfg.containsNode("enabled")) pcfg.insertNode("enabled", "true");
		this.returnWeight = Integer.parseInt(pcfg.getNode("returnWeight").getValue());
		this.enabled = pcfg.getNode("enabled").getValue().equals("true");
	}
	
	public void reload() {
		init();
	}
	
	@Override
	public int check(String ip) {
		return 0;
	}
	
	@Override
	public int check(RequestPacket req) {
		if (!enabled) return 0;
		PluginInline inline = (PluginInline)req.host.getHost().registry.getPatchForClass(PluginInline.class);
		if (!inline.pcfg.getNode("enabled").getValue().equals("true")) {
			Logger.log("[ERROR] Inline is disabled, JLSFlow Security module is unloading!");
			enabled = false;
			return 0;
		}
		
		return 0;
	}
}
