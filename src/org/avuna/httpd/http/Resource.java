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

package org.avuna.httpd.http;

import org.avuna.httpd.http.util.OverrideConfig;

public class Resource {
	
	public byte[] data = new byte[0];
	
	public String type = "text/html";
	
	public String loc = "/";
	
	public boolean wasDir = false;
	
	public boolean tooBig = false;
	
	public OverrideConfig effectiveOverride = null;
	public String oabs = "";// rewrite stuff
	
	public Resource clone() {
		Resource res = new Resource(data, type, loc, effectiveOverride, oabs);
		res.wasDir = wasDir;
		res.tooBig = tooBig;
		return res;
	}
	
	public Resource(byte[] data, String type) {
		this.data = data;
		this.type = type;
	}
	
	public Resource(byte[] data, String type, String loc, OverrideConfig effectiveOverride, String oabs) {
		this.data = data;
		this.type = type;
		this.loc = loc;
		this.effectiveOverride = effectiveOverride;
		this.oabs = oabs;
	}
}
