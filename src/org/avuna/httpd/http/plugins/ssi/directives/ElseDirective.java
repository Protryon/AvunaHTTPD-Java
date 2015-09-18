/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */
package org.avuna.httpd.http.plugins.ssi.directives;

import org.avuna.httpd.http.plugins.ssi.Page;
import org.avuna.httpd.http.plugins.ssi.ParsedSSIDirective;
import org.avuna.httpd.http.plugins.ssi.SSIDirective;
import org.avuna.httpd.http.plugins.ssi.SSIEngine;

public class ElseDirective extends SSIDirective {
	
	public ElseDirective(SSIEngine engine) {
		super(engine);
	}
	
	@Override
	public String call(Page page, ParsedSSIDirective dir) {
		if (!page.lifc.contains(page.scope - 1)) {
			page.returnScopes.remove((Integer) (page.scope - 1));
			page.lifc.add(page.scope - 1);
		}else {
			if (!page.returnScopes.contains(page.scope - 1)) page.returnScopes.add((Integer) (page.scope - 1));
			// do nothing
		}
		return "";
	}
	
	@Override
	public String getDirective() {
		return "else";
	}
	
	public int scopeType() {
		return 3;
	}
}
