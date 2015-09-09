package org.avuna.httpd.http.plugins.ssi.functions;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.avuna.httpd.http.plugins.ssi.Page;
import org.avuna.httpd.http.plugins.ssi.SSIFunction;

public class EscapeFunction extends SSIFunction {
	
	@Override
	public String call(Page page, String arg) {
		try {
			return URLEncoder.encode(arg, "UTF-8");
		}catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
