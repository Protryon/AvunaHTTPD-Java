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
    along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package org.avuna.httpd.http.util;

public enum Directive {
	forbid("forbid"), redirect("redirect"), index("index"), mime("mime"), cache("cache"), rewrite("rewrite");
	public final String name;
	
	Directive(String name) {
		this.name = name;
	}
	
	public static Directive getDirective(String name) {
		for (Directive d : Directive.values()) {
			if (d.name.equals(name)) return d;
		}
		return null;
	}
}
