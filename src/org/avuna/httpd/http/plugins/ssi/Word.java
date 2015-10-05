package org.avuna.httpd.http.plugins.ssi;

public class Word {
	public String value = null;
	public int endIndex = 0;
	
	public Word(String expr, Page page, ParsedSSIDirective dir) {
		if (value == null) try { // digits
			String sv = expr.substring(0, expr.contains(" ") ? expr.indexOf(" ") : expr.length());
			value = Integer.parseInt(sv) + "";
			endIndex = sv.length();
			readForConcat(expr, page, dir);
			return;
		}catch (NumberFormatException e) {
			value = null;
		}
		if (value == null && expr.startsWith("'")) {
			boolean inv = false;
			for (int i = 1; i < expr.length(); i++) {
				char c = expr.charAt(i);
				if (c == '\\') {
					inv = !inv;
				}else {
					if (!inv && c == '\'') {
						value = expr.substring(1, i);
						endIndex = i + 1;
						value = new SSIString(value, page, dir).value;
						readForConcat(expr, page, dir);
						return;
					}
					inv = false;
				}
			}
		}
		if (value == null && expr.startsWith("\"")) {
			boolean inv = false;
			for (int i = 1; i < expr.length(); i++) {
				char c = expr.charAt(i);
				if (c == '\\') {
					inv = !inv;
				}else {
					if (!inv && c == '"') {
						value = expr.substring(1, i);
						endIndex = i + 1;
						value = new SSIString(value, page, dir).value;
						readForConcat(expr, page, dir);
						return;
					}
					inv = false;
				}
			}
		}
		if (value == null && expr.startsWith("%{")) {
			int e = expr.indexOf("}");
			if (e > 2) {
				String name = expr.substring(2, e);
				endIndex = e + 1;
				if (name.contains(":")) {
					String fn = name.substring(0, name.indexOf(":"));
					String fv = name.substring(fn.length() + 1);
					value = page.engine.callFunction(fn, page, fv);
				}else {
					value = page.variables.get(name);
				}
				if (value == null) value = "null";
				readForConcat(expr, page, dir);
				return;
			}else {
				value = null;
			}
		}
		if (value == null && expr.startsWith("$")) {
			if (page.lastMatch == null) {
				value = "";
				readForConcat(expr, page, dir);
				return;
			}else {
				try {
					int val = Integer.parseInt(expr.substring(1, 2));
					if (page.lastMatch.groupCount() > val || val < 0) {
						value = "";
					}else {
						value = page.lastMatch.group(val);
					}
					readForConcat(expr, page, dir);
					return;
					
				}catch (NumberFormatException e) {
					value = null;
				}
			}
		}
		if (value == null) {
			throw new IllegalArgumentException("Bad or unsupported IF word! " + expr);
		}
	}
	
	private void readForConcat(String expr, Page page, ParsedSSIDirective dir) {
		if (expr.length() > endIndex) {
			String af = expr.substring(endIndex).trim();
			if (af.startsWith(".")) {
				af = af.substring(1);
				Word w = new Word(af, page, dir);
				value += w.value;
				endIndex = w.endIndex + (expr.length() - af.length());
			}
		}
	}
}