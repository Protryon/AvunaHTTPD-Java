/*	Avuna HTTPD - General Server Applications
    Copyright (C) 2015 Maxwell Bruce

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
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
				String cr = sd.call(page, dir);
				if (sd.isScope()) {
					if (sd.scopeType()) {
						page.scope++;
					}else {
						page.scope--;
					}
				}
				return cr;
			}
		}
		return null;
	}
	
}
