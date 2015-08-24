package org.avuna.httpd.http.plugins.ssi;

public class ParsedSSIDirective {
	public final String directive;
	public final String[] args;
	public final int start, end;
	
	public ParsedSSIDirective(String directive, String[] args, int start, int end) {
		this.directive = directive;
		this.args = args;
		this.start = start;
		this.end = end;
	}
}