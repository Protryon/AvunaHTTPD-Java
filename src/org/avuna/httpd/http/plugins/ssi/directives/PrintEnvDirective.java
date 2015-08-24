package org.avuna.httpd.http.plugins.ssi.directives;

import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.http.plugins.ssi.Page;
import org.avuna.httpd.http.plugins.ssi.ParsedSSIDirective;
import org.avuna.httpd.http.plugins.ssi.PluginSSI;
import org.avuna.httpd.http.plugins.ssi.SSIDirective;

public class PrintEnvDirective extends SSIDirective {
	
	public PrintEnvDirective(PluginSSI ssi) {
		super(ssi);
	}
	
	@Override
	public String call(Page page, ParsedSSIDirective dir) {
		StringBuilder sb = new StringBuilder();
		for (String key : page.variables.keySet()) {
			sb.append(key).append(" = ").append(page.variables.get(key)).append("<br>").append(AvunaHTTPD.crlf);
		}
		return sb.toString();
	}
	
	@Override
	public String getDirective() {
		return "printenv";
	}
	
}
