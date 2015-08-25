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

public abstract class SSIDirective {
	
	public final PluginSSI ssi;
	
	public SSIDirective(PluginSSI ssi) {
		this.ssi = ssi;
	}
	
	/** Calls a SSI Directive.
	 * 
	 * @param args Arguments to directives, String[], contents formatted name=value
	 * @return If null, will fire an SSI error, if length > 0, the contents will be added to the location of the directive. */
	public abstract String call(Page page, ParsedSSIDirective dir);
	
	public abstract String getDirective();
	
	/** If true, will be treated as a scope opener/closer. */
	public boolean isScope() {
		return false;
	}
	
	/** If true, opens scope, if false, closes scope. */
	public boolean scopeType() {
		return false;
	}
}
