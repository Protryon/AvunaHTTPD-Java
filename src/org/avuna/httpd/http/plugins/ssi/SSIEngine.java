/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */
package org.avuna.httpd.http.plugins.ssi;

import java.util.ArrayList;
import java.util.HashMap;
import org.avuna.httpd.http.plugins.UnaryOP;

/** The purpose of abstracting the SSI Engine is to allow things like template systems to extend SSI, very useful in Avuna Agents. */
public final class SSIEngine {
	public SSIEngine(SSIParser parser) {
		this.parser = parser;
		parser.setEngine(this);
	}
	
	private final ArrayList<SSIDirective> directives = new ArrayList<SSIDirective>();
	private final HashMap<String, SSIFunction> functions = new HashMap<String, SSIFunction>();
	private final HashMap<Character, UnaryOP> unaryops = new HashMap<Character, UnaryOP>();
	private final SSIParser parser;
	
	public String callFunction(String name, Page page, String arg) {
		String nt = name.trim();
		for (String funct : functions.keySet()) {
			if (funct.equalsIgnoreCase(nt)) {
				return functions.get(funct).call(page, arg.trim());
			}
		}
		return null;
	}
	
	public boolean callUnaryOP(char op, Page page, ParsedSSIDirective dir, String arg) {
		for (Character uop : unaryops.keySet()) {
			if (uop == op) {
				return unaryops.get(uop).call(arg.trim(), page, dir);
			}
		}
		return false;
	}
	
	public SSIParser getParser() {
		return parser;
	}
	
	/** Try to only add directives while not calling callDirective. Not thread safe in modification for speed.
	 * 
	 * @param directive The directive to add. */
	public void addDirective(SSIDirective directive) {
		directives.add(directive);
	}
	
	public void addFunction(String name, SSIFunction function) {
		functions.put(name, function);
	}
	
	public void addUnaryOP(char name, UnaryOP uop) {
		unaryops.put(name, uop);
	}
	
	public String callDirective(Page page, ParsedSSIDirective dir) {
		for (SSIDirective sd : directives) {
			if (sd.getDirective().equals(dir.directive)) {
				int st = sd.scopeType();
				int sdd = page.scopeDepth();
				if (sdd == 0 || (sdd == 1 && (st == 3 || st == 2))) {
					String cr = sd.call(page, dir);
					if (st == 1) {
						page.scope++;
					}else if (st == 2) {
						page.scope--;
					}
					return cr;
				}else if (st == 1) { // out of scope, but increases scope
					page.scope++;
					return "";
				}else if (st == 2) {
					page.scope--;
					return "";
				}else { // out of scope
					return "";
				}
			}
		}
		return null;
	}
	
}
