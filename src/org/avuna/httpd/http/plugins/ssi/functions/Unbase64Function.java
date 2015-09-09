package org.avuna.httpd.http.plugins.ssi.functions;

import java.io.IOException;
import org.avuna.httpd.http.plugins.ssi.Page;
import org.avuna.httpd.http.plugins.ssi.SSIFunction;
import sun.misc.BASE64Decoder;

public class Unbase64Function extends SSIFunction {
	
	@Override
	public String call(Page page, String arg) {
		try {
			return new String(new BASE64Decoder().decodeBuffer(arg));
		}catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
