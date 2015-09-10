/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */
package org.avuna.httpd.http.plugins.ssi.directives;

import org.avuna.httpd.http.plugins.ssi.Page;
import org.avuna.httpd.http.plugins.ssi.ParsedSSIDirective;
import org.avuna.httpd.http.plugins.ssi.SSIDirective;
import org.avuna.httpd.http.plugins.ssi.SSIEngine;

public class ConfigDirective extends SSIDirective {
	
	public ConfigDirective(SSIEngine engine) {
		super(engine);
	}
	
	@Override
	public String call(Page page, ParsedSSIDirective dir) {
		for (int i = 0; i < dir.args.length; i++) {
			String vn = dir.args[i].substring(0, dir.args[i].indexOf("="));
			String vd = dir.args[i].substring(vn.length() + 1);
			if (vn.equals("timefmt")) {
				// TODO
			}else if (vn.equals("errmsg")) {
				page.variables.put("error", vd);
			}else if (vn.equals("sizefmt")) {
				// TODO
			}else {
				return null;
			}
		}
		return "";
	}
	
	@Override
	public String getDirective() {
		return "config";
	}
	
}
