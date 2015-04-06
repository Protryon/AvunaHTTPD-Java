package org.avuna.httpd.http2.hpack;

public class Header {
	public String name = "";
	public String value = "";
	
	public Header() {
		
	}
	
	public Header(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	public int sizeHPACK() {
		return name.length() + value.length() + 32;
	}
}
