package org.avuna.httpd.http.plugins.ssi.functions;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import org.avuna.httpd.http.plugins.ssi.Page;
import org.avuna.httpd.http.plugins.ssi.SSIFunction;

public class UnescapeFunction extends SSIFunction {
	
	@Override
	public String call(Page page, String arg) {
		try {
			return URLDecoder.decode(arg, "UTF-8");
		}catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
