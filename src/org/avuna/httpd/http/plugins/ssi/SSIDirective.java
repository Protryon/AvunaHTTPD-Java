package org.avuna.httpd.http.plugins.ssi;

public abstract class SSIDirective {
	
	public final PluginSSI ssi;
	
	public SSIDirective(PluginSSI ssi) {
		this.ssi = ssi;
	}
	
	/** Calls a SSI Directive.
	 * 
	 * @param args Arguments to directives, String[], contents formatted name=value
	 * @return If null, will fire an SSI error, if length > 0, the contents will be added to the location of the directive. */
	public abstract String call(Page page, ParsedSSIDirective dir);
	
	public abstract String getDirective();
}
