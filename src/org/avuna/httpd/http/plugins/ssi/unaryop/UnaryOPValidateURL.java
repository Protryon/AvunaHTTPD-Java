package org.avuna.httpd.http.plugins.ssi.unaryop;

import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.plugins.UnaryOP;
import org.avuna.httpd.http.plugins.ssi.Page;
import org.avuna.httpd.http.plugins.ssi.ParsedSSIDirective;

public class UnaryOPValidateURL extends UnaryOP {
	
	@Override
	public boolean call(String value, Page page, ParsedSSIDirective dir) {
		if (page.data == null || !(page.data instanceof RequestPacket)) return false;
		String rp = value;
		int r = rp.indexOf("://");
		if (r > 0) rp = rp.substring(r + 3);
		r = rp.indexOf("/");
		if (r > 0) rp = rp.substring(r); // ignores scheme and url
		return AvunaHTTPD.fileManager.getResource(rp, ((RequestPacket) page.data)) != null;
	}
	
}
