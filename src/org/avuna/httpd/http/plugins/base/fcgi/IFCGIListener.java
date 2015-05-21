package org.avuna.httpd.http.plugins.base.fcgi;

import org.avuna.httpd.http.plugins.base.fcgi.packets.FCGIPacket;

public interface IFCGIListener {
	public void receive(FCGIPacket packet);
	
	public boolean wants(int id);
}
