/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins.avunaagent.security;

import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.plugins.avunaagent.AvunaAgentSecurity;
import org.avuna.httpd.util.ConfigNode;

public class JLSCompression extends AvunaAgentSecurity {
	
	private int returnWeight = 0;
	private boolean enabled = true;
	
	public void formatConfig(ConfigNode map) {
		if (!map.containsNode("returnWeight")) map.insertNode("returnWeight", "25");
		if (!map.containsNode("enabled")) map.insertNode("enabled", "true");
		this.returnWeight = Integer.parseInt(map.getNode("returnWeight").getValue());
		this.enabled = map.getNode("enabled").getValue().equals("true");
	}
	
	public void reload() {
		init();
	}
	
	@Override
	public int check(String ip) {
		return 0;
	}
	
	@Override
	public int check(RequestPacket req) {
		if (!enabled) return 0;
		String ae = req.headers.getHeader("Accept-Encoding");
		if (!req.headers.hasHeader("Accept-Encoding") || ae == null || !ae.contains("gzip")) {
			return returnWeight;
		}
		return 0;
	}
}
