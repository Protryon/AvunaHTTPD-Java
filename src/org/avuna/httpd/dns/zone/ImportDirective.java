package org.avuna.httpd.dns.zone;

public class ImportDirective extends Directive {
	public ZoneFile zf = null;
	
	public ImportDirective(String[] args, ZoneFile zf) {
		super("import", args);
		this.zf = zf;
	}
}
