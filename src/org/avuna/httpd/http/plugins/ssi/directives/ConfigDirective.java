package org.avuna.httpd.http.plugins.ssi.directives;

import org.avuna.httpd.http.plugins.ssi.Page;
import org.avuna.httpd.http.plugins.ssi.ParsedSSIDirective;
import org.avuna.httpd.http.plugins.ssi.PluginSSI;
import org.avuna.httpd.http.plugins.ssi.SSIDirective;

public class ConfigDirective extends SSIDirective {
	
	public ConfigDirective(PluginSSI ssi) {
		super(ssi);
	}
	
	@Override
	public String call(Page page, ParsedSSIDirective dir) {
		for (int i = 0; i < dir.args.length; i++) {
			String vn = dir.args[i].substring(0, dir.args[i].indexOf("="));
			String vd = dir.args[i].substring(vn.length() + 1);
			if (vn.equals("timefmt")) {
				// TODO
			}else if (vn.equals("errmsg")) {
				page.variables.put("error", vd);
			}else if (vn.equals("sizefmt")) {
				// TODO
			}else {
				return null;
			}
		}
		return "";
	}
	
	@Override
	public String getDirective() {
		return "config";
	}
	
}
