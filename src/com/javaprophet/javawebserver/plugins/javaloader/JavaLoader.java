package com.javaprophet.javawebserver.plugins.javaloader;

import java.util.LinkedHashMap;
import com.javaprophet.javawebserver.hosts.VHost;

public abstract class JavaLoader {
	public JavaLoader() {
		
	}
	
	public void destroy() {
		
	}
	
	public void init(VHost host) {
	}
	
	public void init(VHost host, LinkedHashMap<String, Object> cfg) {
		init(host);
	}
	
	public int getType() {
		return -1;
	}
	
	public void reload(LinkedHashMap<String, Object> cfg) {
		reload();
	}
	
	public void reload() {
		
	}
	
}
