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

package org.avuna.httpd.http.networking.httpm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import org.avuna.httpd.util.unixsocket.UnixSocket;

public class MasterConn {
	private final Socket s;
	private final UnixSocket us;
	private final DataOutputStream out;
	private final DataInputStream in;
	
	public MasterConn(Socket s) throws IOException {
		this.s = s;
		this.us = null;
		this.out = new DataOutputStream(s.getOutputStream());
		this.out.flush();
		this.in = new DataInputStream(s.getInputStream());
	}
	
	public Socket getSocket() {
		return this.s == null ? this.us : this.s;
	}
	
	public MasterConn(UnixSocket us) throws IOException {
		this.us = us;
		this.s = null;
		this.out = new DataOutputStream(us.getOutputStream());
		this.out.flush();
		this.in = new DataInputStream(us.getInputStream());
	}
	
	public DataOutputStream getOutputStream() throws IOException {
		return out;
	}
	
	public DataInputStream getInputStream() throws IOException {
		return in;
	}
}
