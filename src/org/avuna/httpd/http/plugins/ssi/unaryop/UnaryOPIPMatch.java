package org.avuna.httpd.http.plugins.ssi.unaryop;

import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.plugins.UnaryOP;
import org.avuna.httpd.http.plugins.ssi.Page;
import org.avuna.httpd.http.plugins.ssi.ParsedSSIDirective;

public class UnaryOPIPMatch extends UnaryOP {
	
	@Override
	public boolean call(String value, Page page, ParsedSSIDirective dir) {
		if (page.data == null || !(page.data instanceof RequestPacket)) return false;
		return value.equals(((RequestPacket) page.data).userIP);
	}
	
}
