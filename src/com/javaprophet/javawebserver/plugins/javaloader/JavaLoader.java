package com.javaprophet.javawebserver.plugins.javaloader;

import java.util.LinkedHashMap;
import com.javaprophet.javawebserver.hosts.VHost;

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
