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
package org.avuna.httpd.http.plugins.security;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.UUID;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.plugins.avunaagent.lib.SetCookie;

public class SecurityDatabase {
	private ArrayList<SecurityNibble> nibbles = new ArrayList<SecurityNibble>();
	
	public SecurityDatabase() {
		
	}
	
	public static int calcIP(String ip) {
		try {
			return calcIP(InetAddress.getByName(ip).getAddress());
		}catch (UnknownHostException e) {
			// Logger.logError(e);
			return 0;
		}
	}
	
	public static int calcIP(byte[] ip) {
		int iip = Integer.MIN_VALUE;
		iip += ip[0];
		iip += ip[1] * 256;
		iip += ip[2] * 65536;
		iip += ip[3] * 16777216;
		return iip;
	}
	
	public SecurityNibble preIdentify(int ip) {
		for (SecurityNibble nibble : nibbles) {
			for (int nip : nibble.ips) {
				if (nip == ip) return nibble;
			}
		}
		SecurityNibble nibble = new SecurityNibble();
		nibble.addIP(ip);
		return nibble;
	}
	
	public SecurityNibble identify(RequestPacket request) {
		int ip = calcIP(request.userIP);
		SecurityNibble fn = null;
		for (SecurityNibble nibble : nibbles) {
			for (int nip : nibble.ips) {
				if (nip == ip) fn = nibble;
			}
		}
		String avsec = request.cookie.get("avsec");
		if (avsec != null) {
			boolean l = false;
			SecurityNibble ffn = null;
			m: for (SecurityNibble nibble : nibbles) {
				for (int i = 0; i < nibble.session.length; i++) {
					if (nibble.session[i].equals(avsec)) {
						if (fn == null) {
							fn = nibble;
						}else {
							ffn = nibble;
						}
						l = (i == nibble.session.length - 1);
						break m;
					}
				}
			}
			if (ffn != null && ffn != fn) {
				// conflicting IP & cookie, minor issue, likely a traveling cell user or revolving proxy, loss of proxy, or change in location
				// could be a malicious user reusing the same security token to bypass security. take note.
				// we run with the IP based one
			}
		}else { // no cookie
			avsec = UUID.randomUUID().toString();
			new SetCookie(request.child).setCookie("avsec", avsec);
			if (fn == null) {
				fn = new SecurityNibble();
			}
			fn.addSession(avsec);
		}
		if (avsec != null && fn == null) {
			// invalid avsec cookie
			avsec = UUID.randomUUID().toString();
			new SetCookie(request.child).setCookie("avsec", avsec);
			fn = new SecurityNibble();
			fn.addSession(avsec);
		}
		return fn;
	}
}
