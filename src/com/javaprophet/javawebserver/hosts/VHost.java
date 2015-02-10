package com.javaprophet.javawebserver.hosts;

import java.io.File;

public class VHost {
	private final Host host;
	private final File htdocs, htsrc;
	private final String name, vhost;
	
	public VHost(String name, Host host, File htdocs, File htsrc, String vhost) {
		this.name = name;
		this.host = host;
		this.htdocs = htdocs;
		this.htsrc = htsrc;
		this.vhost = vhost;
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
