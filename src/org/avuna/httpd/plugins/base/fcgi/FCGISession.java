package org.avuna.httpd.plugins.base.fcgi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.avuna.httpd.plugins.base.fcgi.packets.Begin;
import org.avuna.httpd.plugins.base.fcgi.packets.EmptyParam;
import org.avuna.httpd.plugins.base.fcgi.packets.FCGIPacket;
import org.avuna.httpd.plugins.base.fcgi.packets.Input;
import org.avuna.httpd.plugins.base.fcgi.packets.NameValue11;
import org.avuna.httpd.plugins.base.fcgi.packets.Stream;

public class FCGISession implements IFCGIListener {
	private static final boolean[] ids = new boolean[65534];
	private int id = -1;
	private ByteArrayOutputStream response = new ByteArrayOutputStream();
	
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
	
	public void param(String name, String value) throws IOException {
		if (fp) {
			throw new IOException("Params are already finished!");
		}
		if (value.length() < 128) {
			conn.write(this, new NameValue11(name, value, id));
		}else {
			// conn.write(this, new NameValue14(name, value, id));
		}
	}
	
	private boolean fp = false;
	
	public void finishParams() throws IOException {
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
	
	private boolean done = false;
	
	@Override
	public void receive(FCGIPacket packet) {
		if (packet.type == Type.FCGI_END_REQUEST) {
			ids[id - 1] = false;
			conn.disassemble(this);
			done = true;
		}else {
			if (packet.type == Type.FCGI_STDOUT || packet.type == Type.FCGI_STDERR) {
				try {
					response.write(((Stream)packet).content);
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
