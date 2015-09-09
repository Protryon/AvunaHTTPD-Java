package org.avuna.httpd.http.plugins.ssi.functions;

import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.plugins.ssi.Page;
import org.avuna.httpd.http.plugins.ssi.SSIFunction;

public class RespFunction extends SSIFunction {
	
	@Override
	public String call(Page page, String arg) {
		if (page.data != null && page.data instanceof RequestPacket) {
			RequestPacket req = (RequestPacket) page.data;
			String hv = req.child.headers.getHeader(arg);
			return hv == null ? "undefined" : hv;
		}
		return "undefined";
	}
	
}
