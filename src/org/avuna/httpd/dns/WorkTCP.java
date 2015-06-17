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

package org.avuna.httpd.dns;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class WorkTCP extends Work {
	protected final Socket s;
	protected final DataInputStream in;
	protected final DataOutputStream out;
	
	public WorkTCP(Socket s, DataInputStream in, DataOutputStream out) {
		super(false);
		this.s = s;
		this.in = in;
		this.out = out;
	}
}
