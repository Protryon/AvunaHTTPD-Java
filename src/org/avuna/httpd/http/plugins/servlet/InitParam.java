package org.avuna.httpd.http.plugins.servlet;

public class InitParam {
	public final String name;
	public final String value;
	public final String desc;
	
	public InitParam(String name, String value, String desc) {
		this.name = name;
		this.value = value;
		this.desc = desc;
	}
}
