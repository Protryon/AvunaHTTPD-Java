package org.avuna.httpd.http.plugins;

import org.avuna.httpd.http.plugins.ssi.Page;
import org.avuna.httpd.http.plugins.ssi.ParsedSSIDirective;

public abstract class UnaryOP {
	public abstract boolean call(String value, Page page, ParsedSSIDirective dir);
}
