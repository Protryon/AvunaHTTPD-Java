package org.avuna.httpd.dns.zone;

public class Directive implements IDirective {
	public String name;
	public String[] args;
	
	public Directive(String name, String[] args) {
		this.name = name;
		this.args = args;
	}
}
