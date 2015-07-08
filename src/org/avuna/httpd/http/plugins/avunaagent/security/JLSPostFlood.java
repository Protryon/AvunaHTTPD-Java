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

package org.avuna.httpd.http.plugins.avunaagent.security;

import org.avuna.httpd.http.Method;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.plugins.avunaagent.AvunaAgentSecurity;

public class JLSPostFlood extends AvunaAgentSecurity {
	
	private int returnWeight = 0;
	private boolean enabled = true;
	
	public void init() {
		if (!pcfg.containsNode("returnWeight")) pcfg.insertNode("returnWeight", "100");
		if (!pcfg.containsNode("enabled")) pcfg.insertNode("enabled", "true");
		this.returnWeight = Integer.parseInt(pcfg.getNode("returnWeight").getValue());
		this.enabled = pcfg.getNode("enabled").getValue().equals("true");
	}
	
	public void reload() {
		init();
	}
	
	@Override
	public int check(RequestPacket req) {
		if (!enabled) return 0;
		if (req.method == Method.POST) {
			if (req.headers.hasHeader("Content-Type")) {
				if (req.headers.getHeader("Content-Type").startsWith("application/x-www-form-urlencoded")) {
					String body = new String(req.body.data);
					if (!body.contains("=") && body.length() > 0) {
						return returnWeight;
					}
				}
			}else {
				return returnWeight;
			}
		}
		return 0;
	}
	
	@Override
	public int check(String arg0) {
		return 0;
	}
	
}
