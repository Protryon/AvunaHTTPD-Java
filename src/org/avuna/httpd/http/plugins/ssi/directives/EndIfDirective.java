package org.avuna.httpd.http.plugins.ssi.directives;

import org.avuna.httpd.http.plugins.ssi.Page;
import org.avuna.httpd.http.plugins.ssi.ParsedSSIDirective;
import org.avuna.httpd.http.plugins.ssi.PluginSSI;
import org.avuna.httpd.http.plugins.ssi.SSIDirective;

public class EndIfDirective extends SSIDirective {
	
	public EndIfDirective(PluginSSI ssi) {
		super(ssi);
	}
	
	@Override
	public String call(Page page, ParsedSSIDirective dir) {
		return null;
	}
	
	@Override
	public String getDirective() {
		return "endif";
	}
	
}
