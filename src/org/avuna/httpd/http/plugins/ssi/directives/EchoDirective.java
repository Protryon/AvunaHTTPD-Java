/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */
package org.avuna.httpd.http.plugins.ssi.directives;

import org.avuna.httpd.http.plugins.ssi.Page;
import org.avuna.httpd.http.plugins.ssi.ParsedSSIDirective;
import org.avuna.httpd.http.plugins.ssi.SSIDirective;
import org.avuna.httpd.http.plugins.ssi.SSIEngine;
import org.avuna.httpd.http.plugins.ssi.Word;

public class EchoDirective extends SSIDirective {
	
	public EchoDirective(SSIEngine engine) {
		super(engine);
	}
	
	@Override
	public String call(Page page, ParsedSSIDirective dir) {
		if (dir.args.length != 1) return null;
		String var = dir.args[0];
		if (var.startsWith("var=")) {
			var = var.substring(4);
			var = page.variables.get(var);
			return var == null ? "" : var;
		}else if (var.startsWith("value=")) {
			String val = var.substring(6);
			return new Word(val, page, dir).value;
		}else {
			return null;
		}
	}
	
	@Override
	public String getDirective() {
		return "echo";
	}
	
}
