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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.avuna.httpd.http.plugins.base.fcgi.packets.Abort;
import org.avuna.httpd.http.plugins.base.fcgi.packets.Begin;
import org.avuna.httpd.http.plugins.base.fcgi.packets.EmptyParam;
import org.avuna.httpd.http.plugins.base.fcgi.packets.FCGIPacket;
import org.avuna.httpd.http.plugins.base.fcgi.packets.Input;
import org.avuna.httpd.http.plugins.base.fcgi.packets.Params;
import org.avuna.httpd.http.plugins.base.fcgi.packets.Stream;

public class FCGISession implements IFCGIListener {
	private static final boolean[] ids = new boolean[65534];
	private int id = -1;
	private ByteArrayOutputStream response = new ByteArrayOutputStream(), error = new ByteArrayOutputStream();
	
	private final FCGIConnection conn;
	
	public FCGISession(FCGIConnection conn) {
		this.conn = conn;
		for (int i = 0; i < ids.length; i++) {
			if (ids[i] == false) {
				this.id = i + 1;
				ids[i] = true;
				break;
			}
		}
		// TODO: if no id avail, wait for one
	}
	
	public void start() throws IOException {
		conn.write(this, new Begin(Role.FCGI_RESPONDER, id));
	}
	
	private ByteArrayOutputStream pout = new ByteArrayOutputStream();
	private DataOutputStream dpout = new DataOutputStream(pout);
	
	public void param(String name, String value) throws IOException {
		// Logger.log(this.hashCode() + " param: " + name + " = " + value);
		if (fp) {
			throw new IOException("Params are already finished!");
		}
		if (name.length() < 128) {
			dpout.write(name.length());
		}else {
			dpout.writeInt(name.length() | 0x80000000);
		}
		if (value.length() < 128) {
			dpout.write(value.length());
		}else {
			dpout.writeInt(value.length() | 0x80000000);
		}
		dpout.write(name.getBytes());
		dpout.write(value.getBytes());
	}
	
	private boolean fp = false;
	
	public void finishParams() throws IOException {
		conn.write(this, new Params(pout.toByteArray(), id));
		conn.write(this, new EmptyParam(id));
		fp = true;
	}
	
	private boolean hasWrittenIn = false;
	
	public void finishReq() throws IOException {
		if (!fp) {
			throw new IOException("FCGISession.finishReq() expects you to call finishParams() first.");
		}
		if (!hasWrittenIn) {
			Input inp = new Input(id);
			inp.content = new byte[0];
			conn.write(this, inp);
		}
		finishReq = true;
	}
	
	private boolean finishReq = false;
	
	public void data(byte[] data) throws IOException {
		if (finishReq) {
			throw new IOException("FCGISession.data() cannot be called after finishReq().");
		}
		if (!fp) {
			throw new IOException("FCGISession.data() expects you to call finishParams() first.");
		}
		if (data.length == 0) hasWrittenIn = true; // TODO: 0 then not 0?
		Input inp = new Input(id);
		inp.content = data;
		conn.write(this, inp);
	}
	
	public boolean isDone() {
		return done;
	}
	
	public byte[] getResponse() {
		return response.toByteArray();
	}
	
	public byte[] getError() {
		return error.toByteArray();
	}
	
	public void abort() throws IOException {
		conn.write(this, new Abort(id));
		// ids[id - 1] = false;
		// conn.disassemble(this);
		// done = true;
	}
	
	private boolean done = false;
	
	@Override
	public void receive(FCGIPacket packet) {
		if (packet.type == Type.FCGI_END_REQUEST) {
			ids[id - 1] = false;
			conn.disassemble(this);
			done = true;
		}else {
			if (packet.type == Type.FCGI_STDOUT) {
				try {
					response.write(((Stream)packet).content);
				}catch (IOException e) {
					// doesnt happen
				}
			}else if (packet.type == Type.FCGI_STDERR) {
				try {
					error.write(((Stream)packet).content);
				}catch (IOException e) {
					// doesnt happen
				}
			}
		}
	}
	
	@Override
	public boolean wants(int id) {
		return id == this.id;
	}
}
