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

package org.avuna.httpd.http.plugins.base.fcgi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FCGIConnectionManagerNMPX implements IFCGIManager {
	private final String ip;
	private final int port;
	private final boolean unix;
	private final List<AugFCGIConnection> vmpx = Collections.synchronizedList(new ArrayList<AugFCGIConnection>());
	
	public FCGIConnectionManagerNMPX(String unix) {
		this.ip = unix;
		this.port = -1;
		this.unix = true;
	}
	
	public FCGIConnectionManagerNMPX(String ip, int port) {
		this.ip = ip;
		this.port = port;
		this.unix = false;
	}
	
	public FCGIConnection getNMPX() throws IOException {
		for (AugFCGIConnection conn : vmpx) {
			if (conn.taken) continue;
			conn.taken = true;
			return conn.conn;
		}
		FCGIConnection nc = unix ? new FCGIConnection(ip) : new FCGIConnection(ip, port);
		nc.start();
		AugFCGIConnection anc = new AugFCGIConnection(nc);
		anc.taken = true;
		vmpx.add(anc);
		return anc.conn;
	}
	
	public static final class AugFCGIConnection {
		public final FCGIConnection conn;
		public boolean taken = false;
		
		public AugFCGIConnection(FCGIConnection conn) {
			this.conn = conn;
			conn.aug = this;
		}
	}
	
	@Override
	public void close() throws IOException {
		for (AugFCGIConnection conn : vmpx) {
			conn.conn.close();
		}
		vmpx.clear();
	}
	
	@Override
	public void start() {
		
	}
}
