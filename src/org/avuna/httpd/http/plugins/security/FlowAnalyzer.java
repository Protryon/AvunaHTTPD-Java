package org.avuna.httpd.http.plugins.security;

import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.Work;

public class FlowAnalyzer {
	private final HostHTTP host;
	
	public FlowAnalyzer(HostHTTP host) {
		this.host = host;
	}
	
	public void connect(Work work) {
		
	}
	
	public void request(RequestPacket req) {
		
	}
}
