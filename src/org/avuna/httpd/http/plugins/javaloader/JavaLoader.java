package org.avuna.httpd.http.plugins.javaloader;

import java.util.LinkedHashMap;
import org.avuna.httpd.hosts.VHost;

public abstract class JavaLoader {
	public JavaLoader() {
		
	}
	
	public void destroy() {
		
	}
	
	public LinkedHashMap<String, Object> pcfg = null;
	public VHost host = null;
	
	public void init() {
	}
	
	public int getType() {
		return -1;
	}
	
	public void reload() {
		
	}
	
}
