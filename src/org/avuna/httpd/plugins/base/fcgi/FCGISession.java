package org.avuna.httpd.plugins.base.fcgi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.avuna.httpd.plugins.base.fcgi.packets.Begin;
import org.avuna.httpd.plugins.base.fcgi.packets.FCGIPacket;
import org.avuna.httpd.plugins.base.fcgi.packets.Input;
import org.avuna.httpd.plugins.base.fcgi.packets.NameValue11;
import org.avuna.httpd.plugins.base.fcgi.packets.NameValue14;
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
		if (value.length() < 128) {
			conn.write(this, new NameValue11(name, value, id));
		}else {
			conn.write(this, new NameValue14(name, value, id));
		}
	}
	
	public void data(byte[] data) throws IOException {
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
