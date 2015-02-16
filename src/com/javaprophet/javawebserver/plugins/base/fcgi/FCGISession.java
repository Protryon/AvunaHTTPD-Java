package com.javaprophet.javawebserver.plugins.base.fcgi;

import com.javaprophet.javawebserver.plugins.base.fcgi.packets.FCGIPacket;

public class FCGISession implements IFCGIListener {
	private static final boolean[] ids = new boolean[65534];
	private int id = -1;
	
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
	
	@Override
	public void receive(FCGIPacket packet) {
		if (packet.type == Type.FCGI_END_REQUEST) {
			ids[id - 1] = false;
			conn.disassemble(this);
		}else {
			
		}
	}
	
	@Override
	public boolean wants(int id) {
		return id == this.id;
	}
}
