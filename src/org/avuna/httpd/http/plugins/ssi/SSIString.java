package org.avuna.httpd.http.plugins.ssi;

public class SSIString {
	public String value = null;
	
	public SSIString(String expr, Page page, ParsedSSIDirective dir) {
		boolean esc = false;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < expr.length(); i++) {
			char c = expr.charAt(i);
			String d = null;
			if (c == '\\') {
				esc = !esc;
				if (esc) continue;
			}else if (!esc && c == '$') {
				try {
					int v = Integer.parseInt(expr.substring(i + 1, i + 2));
					d = (page.lastMatch == null || page.lastMatch.groupCount() > v || v < 0) ? "" : page.lastMatch.group(v);
					i++;// 2nd ++ will be done by for loop
				}catch (NumberFormatException e) {
				
				}
			}else if (!esc && c == '%') {
				if (expr.length() > i + 2 && expr.charAt(i + 1) == '{') {
					int ei = expr.indexOf("}", i + 2);
					if (ei < 0) ei = expr.length() - 1;
					String v = expr.substring(i + 2, ei);
					i = ei;// 2nd ++ will be done by for loop
					if (v.contains(":")) {
						String fn = v.substring(0, v.indexOf(":"));
						String fv = v.substring(fn.length() + 1);
						d = page.engine.callFunction(fn, page, fv);
					}else {
						d = page.variables.get(v);
					}
					if (d == null) d = "null";
				}
			}
			if (d == null) {
				sb.append(c);
			}else if (d.length() > 0) {
				sb.append(d);
			}
		}
		this.value = sb.toString();
	}
}