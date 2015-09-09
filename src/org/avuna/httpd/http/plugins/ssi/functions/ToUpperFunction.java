package org.avuna.httpd.http.plugins.ssi.functions;

import org.avuna.httpd.http.plugins.ssi.Page;
import org.avuna.httpd.http.plugins.ssi.SSIFunction;

public class ToUpperFunction extends SSIFunction {
	
	@Override
	public String call(Page page, String arg) {
		return arg.toUpperCase();
	}
	
}
