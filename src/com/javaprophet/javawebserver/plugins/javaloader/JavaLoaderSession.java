package com.javaprophet.javawebserver.plugins.javaloader;

import java.net.URL;
import java.util.HashMap;
import com.javaprophet.javawebserver.hosts.VHost;

public final class JavaLoaderSession {
	private JavaLoaderClassLoader jlcl = null;
	private HashMap<String, String> loadedClasses = new HashMap<String, String>();
	private HashMap<String, JavaLoader> jls = new HashMap<String, JavaLoader>();
	private VHost vhost;
	
	public JavaLoaderSession(VHost vhost, URL[] urls) {
		this.vhost = vhost;
		this.jlcl = new JavaLoaderClassLoader(urls, this.getClass().getClassLoader());
		PatchJavaLoader.sessions.add(this);
	}
	
	public VHost getVHost() {
		return vhost;
	}
	
	public JavaLoaderClassLoader getJLCL() {
		return jlcl;
	}
	
	public HashMap<String, String> getLoadedClasses() {
		return loadedClasses;
	}
	
	public HashMap<String, JavaLoader> getJLS() {
		return jls;
	}
	
	public void unloadJLCL() {
		for (JavaLoader jl : jls.values()) {
			jl.destroy();
		}
		jlcl = null;
		jls = null;
		loadedClasses = null;
		System.gc();
	}
}
