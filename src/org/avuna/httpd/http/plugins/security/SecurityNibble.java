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

public class SecurityNibble {
	public int[] ips = new int[0];
	public RequestPacketShell[] requests = new RequestPacketShell[0];
	public int connections = 0;
	public long banned = 0L;
	public String[] session = new String[0];
	private double trouble = 0D;
	
	public void addRequest(RequestPacketShell req) {
		RequestPacketShell[] nreq = new RequestPacketShell[requests.length + 1];
		System.arraycopy(requests, 0, nreq, 0, requests.length);
		nreq[requests.length] = req;
		requests = nreq;
	}
	
	public void addSession(String sess) {
		String[] nreq = new String[session.length + 1];
		System.arraycopy(session, 0, nreq, 0, session.length);
		nreq[session.length] = sess;
		session = nreq;
	}
	
	public void addIP(int ip) {
		for (int s : ips) {
			if (s == ip) {
				return;
			}
		}
		int[] nips = new int[ips.length + 1];
		System.arraycopy(ips, 0, nips, 0, ips.length);
		nips[ips.length] = ip;
		ips = nips;
	}
	
	public void fireInfraction(double severity) {
		this.trouble += severity;
	}
}
