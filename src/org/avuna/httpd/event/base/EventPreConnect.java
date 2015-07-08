/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.event.base;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import org.avuna.httpd.event.Event;
import org.avuna.httpd.hosts.Host;

public class EventPreConnect extends Event {
	private final Socket s;
	private final DataOutputStream out;
	private final DataInputStream in;
	private final Host host;
	
	public Host getHost() {
		return host;
	}
	
	public Socket getSocket() {
		return s;
	}
	
	public DataInputStream getInputStream() {
		return in;
	}
	
	public DataOutputStream getOutputStream() {
		return out;
	}
	
	public EventPreConnect(Host host, Socket s, DataOutputStream out, DataInputStream in) {
		super(EventID.PRECONNECT);
		this.s = s;
		this.out = out;
		this.in = in;
		this.host = host;
	}
	
}
