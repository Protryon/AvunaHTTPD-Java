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

package org.avuna.httpd.http.plugins.base.fcgi.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.avuna.httpd.http.plugins.base.fcgi.Role;
import org.avuna.httpd.http.plugins.base.fcgi.Type;

public class Begin extends FCGIPacket {
	public Role role = Role.FCGI_RESPONDER;
	
	public Begin(DataInputStream in, int l) throws IOException {
		super(Type.FCGI_BEGIN_REQUEST);
		readContent(in, l);
	}
	
	public Begin(Role role, int id) {
		super(Type.FCGI_BEGIN_REQUEST, id);
		this.role = role;
	}
	
	@Override
	protected void readContent(DataInputStream in, int l) throws IOException {
		role = Role.fromID(in.readUnsignedShort());
		in.read();// flags
		in.readFully(new byte[5]); // reserved
	}
	
	@Override
	protected void writeContent(DataOutputStream out) throws IOException {
		out.writeShort(role.id);
		out.write(1);// no shutdown
		out.write(new byte[5]); // reserved
	}
	
}
