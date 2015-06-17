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
import org.avuna.httpd.http.plugins.base.fcgi.PStatus;
import org.avuna.httpd.http.plugins.base.fcgi.Type;

public class End extends FCGIPacket {
	public int appStatus = -1;
	public PStatus pstatus = null;
	
	public End(DataInputStream in, int l) throws IOException {
		super(Type.FCGI_END_REQUEST);
		readContent(in, l);
	}
	
	public End(int appStatus, PStatus pstatus, int id) {
		super(Type.FCGI_END_REQUEST, id);
		this.appStatus = appStatus;
		this.pstatus = pstatus;
	}
	
	@Override
	protected void readContent(DataInputStream in, int l) throws IOException {
		appStatus = in.readInt();
		pstatus = PStatus.fromID(in.read());
		in.readFully(new byte[3]); // reserved
	}
	
	@Override
	protected void writeContent(DataOutputStream out) throws IOException {
		out.writeInt(appStatus);
		out.write(pstatus.id);
		out.write(new byte[3]); // reserved
	}
	
}
