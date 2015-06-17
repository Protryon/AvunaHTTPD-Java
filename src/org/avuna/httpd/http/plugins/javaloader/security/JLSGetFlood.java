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

package org.avuna.httpd.http.plugins.javaloader.security;

import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.plugins.javaloader.JavaLoaderSecurity;

public class JLSGetFlood extends JavaLoaderSecurity {
	
	private int returnWeight = 0;
	private boolean enabled = true;
	private String regex = "";
	
	public void init() {
		if (!pcfg.containsNode("returnWeight")) pcfg.insertNode("returnWeight", "100");
		if (!pcfg.containsNode("enabled")) pcfg.insertNode("enabled", "false");
		if (!pcfg.containsNode("regex")) pcfg.insertNode("regex", "/\\?[0-9a-zA-Z]{2,}");
		this.returnWeight = Integer.parseInt(pcfg.getNode("returnWeight").getValue());
		this.enabled = pcfg.getNode("enabled").getValue().equals("true");
		this.regex = pcfg.getNode("regex").getValue();
	}
	
	public void reload() {
		init();
	}
	
	@Override
	public int check(RequestPacket req) {
		if (!enabled) return 0;
		if (req.target.matches(regex)) {
			return returnWeight;
		}
		return 0;
	}
	
	@Override
	public int check(String arg0) {
		return 0;
	}
	
}
