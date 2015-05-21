package org.avuna.httpd.dns;

import java.net.DatagramSocket;
import java.net.InetAddress;

public class WorkUDP extends Work {
	protected final byte[] query;
	protected final InetAddress rip;
	protected final int rport;
	protected final DatagramSocket server;
	
	protected WorkUDP(byte[] query, InetAddress rip, int rport, DatagramSocket server) {
		super(true);
		this.query = query;
		this.rip = rip;
		this.rport = rport;
		this.server = server;
	}
}
