package org.avuna.httpd.hosts;

public class VHostM extends VHost {
	public final String ip;
	public final int port;
	public final boolean unix;
	
	public VHostM(String name, HostHTTP host, boolean unix, String vhost, String ip, int port) {
		super(name, host, null, null, vhost);
		this.ip = ip;
		this.port = port;
		this.unix = unix;
	}
}
