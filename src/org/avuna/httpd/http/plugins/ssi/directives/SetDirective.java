package org.avuna.httpd.http.plugins.ssi.directives;

import org.avuna.httpd.http.plugins.ssi.Page;
import org.avuna.httpd.http.plugins.ssi.ParsedSSIDirective;
import org.avuna.httpd.http.plugins.ssi.PluginSSI;
import org.avuna.httpd.http.plugins.ssi.SSIDirective;

public class SetDirective extends SSIDirective {
	
	public SetDirective(PluginSSI ssi) {
		super(ssi);
	}
	
	@Override
	public String call(Page page, ParsedSSIDirective dir) {
		if (dir.args.length != 2) return null;
		String var = null, value = null;
		if (dir.args[0].startsWith("var=")) {
			var = dir.args[0].substring(4);
		}else if (dir.args[1].startsWith("var=")) {
			var = dir.args[1].substring(4);
		}
		if (dir.args[0].startsWith("value=")) {
			value = dir.args[0].substring(6);
		}else if (dir.args[1].startsWith("value=")) {
			value = dir.args[1].substring(6);
		}
		// above if statements are nasty.
		if (var == null || value == null) return null;
		page.variables.put(var, value);
		return "";
	}
	
	@Override
	public String getDirective() {
		return "set";
	}
	
}
