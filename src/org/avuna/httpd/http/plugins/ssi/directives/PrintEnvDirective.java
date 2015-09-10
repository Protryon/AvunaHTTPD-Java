/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */
package org.avuna.httpd.http.plugins.ssi.directives;

import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.http.plugins.ssi.Page;
import org.avuna.httpd.http.plugins.ssi.ParsedSSIDirective;
import org.avuna.httpd.http.plugins.ssi.SSIDirective;
import org.avuna.httpd.http.plugins.ssi.SSIEngine;

public class PrintEnvDirective extends SSIDirective {
	
	public PrintEnvDirective(SSIEngine engine) {
		super(engine);
	}
	
	@Override
	public String call(Page page, ParsedSSIDirective dir) {
		StringBuilder sb = new StringBuilder();
		for (String key : page.variables.keySet()) {
			sb.append(key).append(" = ").append(page.variables.get(key)).append("<br>").append(AvunaHTTPD.crlf);
		}
		return sb.toString();
	}
	
	@Override
	public String getDirective() {
		return "printenv";
	}
	
}
