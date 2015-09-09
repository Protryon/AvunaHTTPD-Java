package org.avuna.httpd.http.plugins.ssi.unaryop;

import org.avuna.httpd.http.plugins.UnaryOP;
import org.avuna.httpd.http.plugins.ssi.Page;
import org.avuna.httpd.http.plugins.ssi.ParsedSSIDirective;

public class UnaryOPDirExists extends UnaryOP {
	
	@Override
	public boolean call(String value, Page page, ParsedSSIDirective dir) {
		return false;
	}
	
}
