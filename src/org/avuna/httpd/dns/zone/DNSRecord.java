package org.avuna.httpd.dns.zone;

import org.avuna.httpd.dns.Type;
import org.avuna.httpd.dns.Util;

public class DNSRecord implements IDirective {
	private final String domain;
	private final Type type;
	private final int ttl;
	private final byte[] data;
	
	public String getDomain() {
		return domain;
	}
	
	public Type getType() {
		return type;
	}
	
	public int getTimeToLive() {
		return ttl;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public DNSRecord(String domain, Type type, int ttl, byte[] data) {
		this.domain = domain;
		this.type = type;
		this.ttl = ttl;
		this.data = data;
	}
	
	public DNSRecord(String domain, String ip, int ttl) {
		this.domain = domain;
		this.type = Type.A;
		this.ttl = ttl;
		this.data = Util.ipToByte(ip);
	}
}
