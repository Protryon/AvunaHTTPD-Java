package org.avuna.httpd.dns.zone;

public class ZoneDirective extends Directive {
	public ZoneFile zf = null;
	public String zr;
	
	public ZoneDirective(String[] args, ZoneFile zf) {
		super("zone", args);
		this.zf = zf;
		this.zr = args[0];
	}
}
