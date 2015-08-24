package org.avuna.httpd.http.plugins.ssi;

import java.util.ArrayList;

/** The purpose of abstracting the SSI Engine is to allow things like template systems to extend SSI, very useful in Avuna Agents. */
public final class SSIEngine {
	public SSIEngine() {
	
	}
	
	private final ArrayList<SSIDirective> directives = new ArrayList<SSIDirective>();
	private final SSIParser parser = new SSIParser(this);
	
	public SSIParser getParser() {
		return parser;
	}
	
	/** Try to only add directives while not calling callDirective. Not thread safe in modification for speed.
	 * 
	 * @param directive The directive to add. */
	public void addDirective(SSIDirective directive) {
		directives.add(directive);
	}
	
	public String callDirective(Page page, ParsedSSIDirective dir) {
		for (SSIDirective sd : directives) {
			if (sd.getDirective().equals(dir.directive)) {
				return sd.call(page, dir);
			}
		}
		return null;
	}
	
}
