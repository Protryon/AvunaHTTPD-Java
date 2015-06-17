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
import org.avuna.httpd.http.plugins.base.fcgi.Type;

public abstract class Stream extends FCGIPacket {
	public byte[] content = null;
	
	protected Stream(Type type) {
		super(type);
	}
	
	public Stream(Type type, DataInputStream in, int l) throws IOException {
		this(type);
		readContent(in, l);
	}
	
	public Stream(Type type, int id) {
		super(type, id);
	}
	
	@Override
	protected void readContent(DataInputStream in, int l) throws IOException {
		content = new byte[l];
		in.readFully(content);
	}
	
	@Override
	protected void writeContent(DataOutputStream out) throws IOException {
		out.write(content);
	}
	
}
