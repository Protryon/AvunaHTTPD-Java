package org.avuna.httpd.hosts;

import java.util.ArrayList;

public class Protocol {
	private static final ArrayList<Protocol> protocols = new ArrayList<Protocol>();
	public static final Protocol HTTP = new Protocol("HTTP");
	public static final Protocol HTTPM = new Protocol("HTTPM");
	public static final Protocol MAIL = new Protocol("MAIL");
	public static final Protocol DNS = new Protocol("DNS");
	public static final Protocol COM = new Protocol("COM");
	public static final Protocol FTP = new Protocol("FTP");
	public final String name;
	
	public Protocol(String name) {
		this.name = name;
		protocols.add(this);
	}
	
	public static Protocol fromString(String s) {
		for (Protocol val : protocols) {
			if (val.name.equalsIgnoreCase(s)) return val;
		}
		return null;
	}
}
