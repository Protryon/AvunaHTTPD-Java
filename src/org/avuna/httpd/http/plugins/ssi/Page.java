package org.avuna.httpd.http.plugins.ssi;

import java.util.HashMap;

public class Page {
	public final ParsedSSIDirective[] directives;
	public final HashMap<String, String> variables = new HashMap<String, String>();
	/** An optionally filled variable for metadata. For example, in HTTP, it is a RequestPacket. */
	public Object data = null;
	
	public Page(ParsedSSIDirective[] directives) {
		this.directives = directives;
		variables.put("error", "[an error occurred while processing this directive]");
	}
}
