package com.javaprophet.javawebserver.plugins.base.fcgi;

import com.javaprophet.javawebserver.plugins.base.fcgi.packets.FCGIPacket;

public interface IFCGIListener {
	public void receive(FCGIPacket packet);
	
	public boolean wants(int id);
}
