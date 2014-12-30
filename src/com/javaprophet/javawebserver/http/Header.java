package com.javaprophet.javawebserver.http;

/**
 * Header/key-value thing.
 */
public class Header {
	public String name = "";
	public String value = "";
	
	public Header(String name) {
		this();
		this.name = name;
	}
	
	public Header(String name, String value) {
		this(name);
		this.value = value;
	}
	
	public Header() {
		
	}
	
	public static Header fromLine(String line) {
		if (line.length() <= 2 || !line.contains(":")) {
			return null;
		}
		return new Header(line.substring(0, line.indexOf(":")).trim(), line.substring(line.indexOf(":") + 1).trim());
	}
	
	public String toLine() {
		return name + ": " + value;
	}
}
