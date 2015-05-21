package org.avuna.httpd.hosts;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import org.avuna.httpd.http.plugins.javaloader.JavaLoaderSession;

public class VHost {
	private final HostHTTP host;
	private final File htdocs, htsrc;
	private final String name, vhost;
	private JavaLoaderSession jls;
	private final VHost parent;
	private ArrayList<VHost> children = new ArrayList<VHost>();
	
	public VHost(String name, HostHTTP host, String vhost, VHost parent) {
		this.name = name;
		this.host = host;
		this.htdocs = parent.htdocs;
		this.htsrc = parent.htsrc;
		this.vhost = vhost;
		this.parent = parent;
		parent.children.add(this);
	}
	
	public VHost(String name, HostHTTP host, File htdocs, File htsrc, String vhost) {
		this.name = name;
		this.host = host;
		this.htdocs = htdocs;
		this.htsrc = htsrc;
		this.vhost = vhost;
		this.parent = null;
	}
	
	public boolean isChild() {
		return parent != null;
	}
	
	public void initJLS(URL[] url) {
		if (this.parent == null) {
			this.jls = new JavaLoaderSession(this, url);
			for (VHost child : children) {
				child.jls = this.jls;
			}
		}
	}
	
	public String getName() {
		return name;
	}
	
	private boolean debug = false;
	
	public void setDebug(boolean set) {
		debug = set;
	}
	
	public boolean getDebug() {
		return debug;
	}
	
	public JavaLoaderSession getJLS() {
		return jls;
	}
	
	public String getVHost() {
		return vhost;
	}
	
	public HostHTTP getHost() {
		return host;
	}
	
	public void setupFolders() {
		if (htdocs != null) htdocs.mkdirs();
		if (htsrc != null) htsrc.mkdirs();
	}
	
	public File getHTDocs() {
		return htdocs;
	}
	
	public File getHTSrc() {
		return htsrc;
	}
	
	public String getHostPath() {
		return name;
	}
}
