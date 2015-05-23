package org.avuna.httpd.http.plugins.javaloader;

import org.avuna.httpd.hosts.VHost;
import org.avuna.httpd.util.ConfigNode;

public abstract class JavaLoader {
	public JavaLoader() {
		
	}
	
	public void destroy() {
		
	}
	
	public ConfigNode pcfg = null;
	public VHost host = null;
	
	public void init() {
	}
	
	public void postinit() {
	}
	
	public int getType() {
		return -1;
	}
	
	public void reload() {
		
	}
	
}
