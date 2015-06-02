package org.avuna.httpd.dns.zone;

public class RoundStartDirective extends Directive {
	public int limit = -1;
	
	public RoundStartDirective(String[] args) {
		super("roundstart", args);
		if (args.length == 1) {
			try {
				limit = Integer.parseInt(args[0]);
			}catch (NumberFormatException e) {
				
			}
		}
	}
}
