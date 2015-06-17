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

package org.avuna.httpd.http.plugins.javaloader.lib;

import java.util.HashMap;
import java.util.UUID;
import org.avuna.httpd.http.networking.ResponsePacket;

public class Session {
	private final String csessid;
	private final String useragent;
	private final boolean bindAgent;
	private final HashMap<String, String> data = new HashMap<String, String>();
	
	private Session(ResponsePacket base, boolean bindAgent) {
		this.csessid = UUID.randomUUID().toString();
		this.useragent = base.request.headers.hasHeader("User-Agent") ? base.request.headers.getHeader("User-Agent") : "";
		this.bindAgent = bindAgent;
		base.headers.addHeader("Set-Cookie", "jSessID=" + csessid);
	}
	
	private static final HashMap<String, Session> sesses = new HashMap<String, Session>();
	
	public static Session getSession(ResponsePacket base, boolean bindAgent) {
		if (base.request.cookie.containsKey("jSessID")) {
			Session maybe = sesses.get(base.request.cookie.get("jSessID"));
			if (maybe != null) {
				if (!maybe.bindAgent || maybe.useragent.equals(base.request.headers.hasHeader("User-Agent") ? base.request.headers.getHeader("User-Agent") : "")) {
					return maybe;
				}
			}
		}
		Session n = new Session(base, bindAgent);
		sesses.put(n.csessid, n);
		return n;
	}
	
	public static Session getSession(ResponsePacket base) {
		return getSession(base, true);
	}
	
	public String get(String name) {
		return data.get(name);
	}
	
	public boolean has(String name) {
		return data.containsKey(name);
	}
	
	public Session set(String name, String value) {
		data.put(name, value);
		return this;
	}
	
	public Session remove(String name) {
		data.remove(name);
		return this;
	}
	
	public void clear() {
		data.clear();
	}
}
