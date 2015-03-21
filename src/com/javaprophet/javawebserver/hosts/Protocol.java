package com.javaprophet.javawebserver.hosts;

public enum Protocol {
	HTTP("HTTP"), MAIL("MAIL"), DNS("DNS"), COM("COM");
	public final String name;
	
	private Protocol(String name) {
		this.name = name;
	}
	
	public static Protocol fromString(String s) {
		for (Protocol val : values()) {
			if (val.name.equalsIgnoreCase(s)) return val;
		}
		return null;
	}
}
