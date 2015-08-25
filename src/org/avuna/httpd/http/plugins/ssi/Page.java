package org.avuna.httpd.http.plugins.ssi;

import java.util.HashMap;

public class Page {
	public final ParsedSSIDirective[] directives;
	public final HashMap<String, String> variables = new HashMap<String, String>();
	/** An optionally filled variable for metadata. For example, in HTTP, it is a RequestPacket. */
	public Object data = null;
	public int scope = 0;
	/** If set >= 0, output MUST NOT continue until scope meets this, and then it MUST reset to -1. */
	public int returnScope = -1;
	
	public Page(ParsedSSIDirective[] directives) {
		this.directives = directives;
		variables.put("error", "[an error occurred while processing this directive]");
	}
	
	/** Tells whether we are inside a returned scope, ie a failed IF statement. This does change state, so multiple calls are BAD. */
	public boolean shouldOutputNextBlock() {
		if (returnScope < 0) return true;
		if (returnScope >= scope) {
			returnScope = -1;
			return false;
		}else {
			return true;
		}
	}
}
