package org.avuna.httpd.http.plugins.ssi.unaryop;

import org.avuna.httpd.http.plugins.UnaryOP;
import org.avuna.httpd.http.plugins.ssi.Page;
import org.avuna.httpd.http.plugins.ssi.ParsedSSIDirective;

public class UnaryOPStringTrue extends UnaryOP {
	
	@Override
	public boolean call(String value, Page page, ParsedSSIDirective dir) {
		return !(value == null || value.length() == 0 || value.equals("0") || value.equalsIgnoreCase("off") || value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no"));
	}
	
}
