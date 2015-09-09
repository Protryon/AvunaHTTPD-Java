package org.avuna.httpd.http.plugins.ssi.functions;

import org.avuna.httpd.http.plugins.ssi.Page;
import org.avuna.httpd.http.plugins.ssi.SSIFunction;
import sun.misc.BASE64Encoder;

public class Base64Function extends SSIFunction {
	
	@Override
	public String call(Page page, String arg) {
		return new BASE64Encoder().encode(arg.getBytes());
	}
	
}
