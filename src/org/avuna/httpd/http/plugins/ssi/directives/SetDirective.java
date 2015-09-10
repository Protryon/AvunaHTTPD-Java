/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */
package org.avuna.httpd.http.plugins.ssi.directives;

import org.avuna.httpd.http.plugins.ssi.Page;
import org.avuna.httpd.http.plugins.ssi.ParsedSSIDirective;
import org.avuna.httpd.http.plugins.ssi.SSIDirective;
import org.avuna.httpd.http.plugins.ssi.SSIEngine;

public class SetDirective extends SSIDirective {
	
	public SetDirective(SSIEngine engine) {
		super(engine);
	}
	
	@Override
	public String call(Page page, ParsedSSIDirective dir) {
		if (dir.args.length != 2) return null;
		String var = null, value = null;
		if (dir.args[0].startsWith("var=")) {
			var = dir.args[0].substring(4);
		}else if (dir.args[1].startsWith("var=")) {
			var = dir.args[1].substring(4);
		}
		if (dir.args[0].startsWith("value=")) {
			value = dir.args[0].substring(6);
		}else if (dir.args[1].startsWith("value=")) {
			value = dir.args[1].substring(6);
		}
		// above if statements are nasty.
		if (var == null || value == null) return null;
		page.variables.put(var, value);
		return "";
	}
	
	@Override
	public String getDirective() {
		return "set";
	}
	
}
