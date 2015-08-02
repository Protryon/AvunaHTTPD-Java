/*
 * Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins.avunaagent.security;

import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.plugins.avunaagent.AvunaAgentSecurity;
import org.avuna.httpd.util.ConfigNode;

public class JLSRequestFlood extends AvunaAgentSecurity {
	
	private int maxRequestPerSecond = 0, returnWeight = 0;
	private boolean enabled = true;
	
	public void formatConfig(ConfigNode map) {
		if (!map.containsNode("maxRequestPerSecond")) map.insertNode("maxRequestPerSecond", "64");
		if (!map.containsNode("returnWeight")) map.insertNode("returnWeight", "100");
		if (!map.containsNode("enabled")) map.insertNode("enabled", "true");
		this.maxRequestPerSecond = Integer.parseInt(map.getNode("maxRequestPerSecond").getValue());
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
		double ds = (((double) System.currentTimeMillis() - (double) req.work.rqst) / 1000D);
		if (ds < 1D) return 0; // if under one second, we may get an issue with avuna being TOO fast.
		double rqps = (double) req.work.rqs / ds; // get connection time elapsed(from first request) in seconds, under the number of requests.
		if (rqps > maxRequestPerSecond) { // if the average req/sec if >= the max,
			return (int) (returnWeight * (rqps / (double) maxRequestPerSecond)); // return the amount over, will usually be 1*returnWeight. ex. if they got 128 rq/s, it would return 2*returnWeight by default
		}
		return 0;
	}
}
