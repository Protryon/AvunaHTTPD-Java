package org.avuna.httpd.http.plugins.ssi.directives;

import org.avuna.httpd.http.plugins.ssi.Page;
import org.avuna.httpd.http.plugins.ssi.ParsedSSIDirective;
import org.avuna.httpd.http.plugins.ssi.PluginSSI;
import org.avuna.httpd.http.plugins.ssi.SSIDirective;

public class EchoDirective extends SSIDirective {
	
	public EchoDirective(PluginSSI ssi) {
		super(ssi);
	}
	
	@Override
	public String call(Page page, ParsedSSIDirective dir) {
		if (dir.args.length != 1) return null;
		String var = dir.args[0];
		if (!var.startsWith("var=")) return null;
		var = var.substring(4);
		var = page.variables.get(var);
		return var == null ? "" : var;
	}
	
	@Override
	public String getDirective() {
		return "echo";
	}
	
}
