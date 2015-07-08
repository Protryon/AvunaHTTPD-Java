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

import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.plugins.avunaagent.AvunaAgentSecurity;
import org.avuna.httpd.util.Logger;

public class JLSConnectionFlood extends AvunaAgentSecurity {
	
	private int maxConcurrentConns = 0, returnWeight = 0;
	private boolean enabled = true;
	
	public void init() {
		if (!pcfg.containsNode("maxConcurrentConns")) pcfg.insertNode("maxConcurrentConns", "50");
		if (!pcfg.containsNode("returnWeight")) pcfg.insertNode("returnWeight", "100");
		if (!pcfg.containsNode("enabled")) pcfg.insertNode("enabled", "true");
		this.maxConcurrentConns = Integer.parseInt(pcfg.getNode("maxConcurrentConns").getValue());
		this.returnWeight = Integer.parseInt(pcfg.getNode("returnWeight").getValue());
		this.enabled = pcfg.getNode("enabled").getValue().equals("true");
	}
	
	public void reload() {
		init();
	}
	
	@Override
	public int check(String ip) {
		if (!enabled) return 0;
		int ips = HostHTTP.getConnectionsForIP(ip);
		if (ips > maxConcurrentConns) {
			Logger.log("Connection abuse from " + ip + ", may be banned depending on configuration.");
			return returnWeight;
		}
		return 0;
	}
	
	@Override
	public int check(RequestPacket req) {
		return 0;
	}
}
