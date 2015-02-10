package com.javaprophet.javawebserver.hosts;

import java.io.File;
import java.net.URL;
import com.javaprophet.javawebserver.plugins.javaloader.JavaLoaderSession;

public class VHost {
	private final Host host;
	private final File htdocs, htsrc;
	private final String name, vhost;
	private JavaLoaderSession jls;
	
	public VHost(String name, Host host, File htdocs, File htsrc, String vhost) {
		this.name = name;
		this.host = host;
		this.htdocs = htdocs;
		this.htsrc = htsrc;
		this.vhost = vhost;
	}
	
	public void initJLS(URL[] url) {
		this.jls = new JavaLoaderSession(this, url);
	}
	
	public JavaLoaderSession getJLS() {
		return jls;
	}
	
	public void setJLS(JavaLoaderSession jls) {
		this.jls = jls;
	}
	
	public String getVHost() {
		return vhost;
	}
	
	public Host getHost() {
		return host;
	}
	
	public void setupFolders() {
		htdocs.mkdirs();
		htsrc.mkdirs();
	}
	
	public File getHTDocs() {
		return htdocs;
	}
	
	public File getHTSrc() {
		return htsrc;
	}
	
	public String getHostPath() {
		return host.getHostname() + "/" + name;
	}
}
