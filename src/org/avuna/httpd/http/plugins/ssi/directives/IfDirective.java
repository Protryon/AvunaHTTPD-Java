package org.avuna.httpd.http.plugins.ssi.directives;

import org.avuna.httpd.http.plugins.ssi.Page;
import org.avuna.httpd.http.plugins.ssi.ParsedSSIDirective;
import org.avuna.httpd.http.plugins.ssi.PluginSSI;
import org.avuna.httpd.http.plugins.ssi.SSIDirective;

public class IfDirective extends SSIDirective {
	
	public IfDirective(PluginSSI ssi) {
		super(ssi);
	}
	
	protected static boolean processSBNF(String pexpr, Page page, ParsedSSIDirective dir) {
		String expr = pexpr;
		if (expr.equals("true")) return true;
		if (expr.equals("false")) return false;
		return false;
	}
	
	protected static boolean processBNF(String pexpr, Page page, ParsedSSIDirective dir) {
		String expr = pexpr;
		int scope = 0;
		boolean inq = false;
		char lc = 0;
		int s0s = -1;
		boolean inverted = false;
		for (int i = 0; i < expr.length(); i++) { // process parenthesis
			char c = expr.charAt(i);
			if (lc != '\\') {
				if (!inq && c == '(') {
					if (scope == 0) {
						inverted = (lc == '!');
						s0s = i;
					}
					scope++;
				}else if (!inq && c == ')') {
					scope--;
					if (scope == 0) {
						boolean se = processBNF(expr.substring(s0s + 1, i), page, dir);
						if (inverted) se = !se;
						String nexpr = expr.substring(0, s0s) + (se ? "true" : "false") + expr.substring(i + 1, expr.length());
						i = s0s + (se ? 3 : 4);
						expr = nexpr;
					}
				}else if (c == '\'') {
					inq = !inq;
				}
				lc = c;
			}else if (c == '\\') {
				lc = 0;
			}else {
				lc = c;
			}
		}
		boolean v = true;
		String[] ands = expr.split("&&"); // TODO: quote detection
		for (String and : ands) {
			boolean as = false;
			String[] ors = and.trim().split("\\|\\|");
			for (String or : ors) {
				as = processSBNF(or.trim(), page, dir);
				if (as) break;
			}
			if (!as) {
				v = false;
				break;
			}
		}
		return v;
	}
	
	@Override
	public String call(Page page, ParsedSSIDirective dir) {
		if (dir.args.length != 1) return null;
		if (!dir.args[0].startsWith("expr=")) return null;
		if (processBNF(dir.args[0].substring(5), page, dir)) {
			// do nothing
			System.out.println("true");
		}else {
			System.out.println("false");
			page.returnScope = page.scope;
		}
		return "";
	}
	
	@Override
	public String getDirective() {
		return "if";
	}
	
	public boolean isScope() {
		return true;
	}
	
	public boolean scopeType() {
		return true;
	}
	
}
