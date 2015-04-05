package org.avuna.httpd.hosts;

import java.io.File;

public class VHostM extends VHost {
	public final String ip;
	public final int port;
	
	public VHostM(String name, HostHTTP host, File htdocs, File htsrc, String vhost, String ip, int port) {
		super(name, host, htdocs, htsrc, vhost);
		this.ip = ip;
		this.port = port;
	}
	
	public VHostM(String name, HostHTTP host, String vhost, VHost parent, String ip, int port) {
		super(name, host, vhost, parent);
		this.ip = ip;
		this.port = port;
	}
}
