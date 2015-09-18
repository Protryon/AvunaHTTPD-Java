/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */
package org.avuna.httpd.http.plugins.ssi.directives;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.avuna.httpd.http.plugins.ssi.Page;
import org.avuna.httpd.http.plugins.ssi.ParsedSSIDirective;
import org.avuna.httpd.http.plugins.ssi.SSIDirective;
import org.avuna.httpd.http.plugins.ssi.SSIEngine;
import org.avuna.httpd.http.plugins.ssi.Word;

public class IfDirective extends SSIDirective {
	
	public IfDirective(SSIEngine engine) {
		super(engine);
	}
	
	protected boolean processSBNF(String pexpr, Page page, ParsedSSIDirective dir) {
		String expr = pexpr.trim();
		if (expr.equals("true")) return true;
		if (expr.equals("false")) return false;
		if (expr.startsWith("!")) {
			return !processSBNF(expr.substring(1), page, dir);
		}
		if (expr.startsWith("-")) { // unaryop word
			int si = expr.indexOf(" ");
			if (si < 0) throw new IllegalArgumentException("Bad unary op!");
			String op = expr.substring(1, si);
			expr = expr.substring(si + 1).trim();
			Word w1 = new Word(expr, page, dir);
			expr = pexpr.substring(w1.endIndex).trim();
			if (op.length() > 0) {
				return engine.callUnaryOP(op.charAt(0), page, dir, w1.value);
			}
		}else {
			Word w1 = new Word(expr, page, dir);
			expr = pexpr.substring(w1.endIndex).trim();
			// below is stringcomp
			if (expr.startsWith("==") || expr.startsWith("!=") || expr.startsWith("<") || expr.startsWith("<=") || expr.startsWith(">") || expr.startsWith(">=")) {
				String op = expr.substring(0, 2);
				expr = expr.substring(2).trim();
				Word w2 = new Word(expr, page, dir);
				if (op.equals("==")) {
					return w2.value.equals(w1.value);
				}else if (op.equals("!=")) {
					return !w2.value.equals(w1.value);
				}else {
					throw new IllegalArgumentException("Unimplemented String Comparison!");
				}
			}
			// below is intcomp
			if (expr.startsWith("-")) {
				expr = expr.substring(1);
			}
			// TODO: not case sensitive
			if (expr.startsWith("eq") || expr.startsWith("ne") || expr.startsWith("lt") || expr.startsWith("le") || expr.startsWith("gt") || expr.startsWith("ge")) {
				String op = expr.substring(0, 2);
				expr = expr.substring(2).trim();
				Word w2 = new Word(expr, page, dir);
				if (op.equals("eq")) {
					return Double.parseDouble(w1.value) == Double.parseDouble(w2.value);
				}else if (op.equals("ne")) {
					return Double.parseDouble(w1.value) != Double.parseDouble(w2.value);
				}else if (op.equals("lt")) {
					return Double.parseDouble(w1.value) < Double.parseDouble(w2.value);
				}else if (op.equals("le")) {
					return Double.parseDouble(w1.value) <= Double.parseDouble(w2.value);
				}else if (op.equals("gt")) {
					return Double.parseDouble(w1.value) > Double.parseDouble(w2.value);
				}else if (op.equals("ge")) {
					return Double.parseDouble(w1.value) >= Double.parseDouble(w2.value);
				}// all caught occurrences handled
			}
			// regex
			if (expr.startsWith("=~") || expr.startsWith("!~")) {
				String op = expr.substring(0, 2);
				expr = expr.substring(2).trim();
				Word w2 = new Word(expr, page, dir);
				Pattern p = Pattern.compile(w2.value);
				Matcher m = p.matcher(w1.value);
				page.lastMatch = m;
				if (op.equals("=~")) {
					return m.matches();
				}else if (op.equals("!~")) {
					return !m.matches();
				}
			}
		}
		return false;
	}
	
	protected boolean processBNF(String pexpr, Page page, ParsedSSIDirective dir) {
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
			page.lifc.add(page.scope);
		}else {
			page.returnScopes.add(page.scope);
		}
		return "";
	}
	
	@Override
	public String getDirective() {
		return "if";
	}
	
	public int scopeType() {
		return 1;
	}
	
}
