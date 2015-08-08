/*
 * Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.dns;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import org.avuna.httpd.hosts.HostDNS;
import org.avuna.httpd.hosts.ITerminatable;

/** Created by JavaProphet on 8/13/14 at 10:56 PM. */
public class UDPServer extends Thread implements IServer, ITerminatable {
	private HostDNS host;
	
	public UDPServer(HostDNS host) {
		super("DNS UDPServer");
		this.host = host;
	}
	
	public boolean bound = false;
	private DatagramSocket server = null;
	
	public void run() {
		try {
			server = new DatagramSocket(Integer.parseInt(host.getConfig().getNode("port").getValue()));
			bound = true;
			while (!server.isClosed()) {
				try {
					byte[] rec = new byte[1024];
					final DatagramPacket receive = new DatagramPacket(rec, rec.length);
					server.receive(receive);
					host.addWork(new WorkUDP(receive.getData(), receive.getAddress(), receive.getPort(), server));
				}catch (SocketException e2) {}catch (Exception e) {
					host.logger.logError(e);
				}
			}
		}catch (Exception e) {
			host.logger.logError(e);
		}finally {
			if (server != null) server.close();
			bound = true;
		}
	}
	
	@Override
	public void terminate() {
		if (server != null) {
			server.close();
		}
		this.interrupt();
	}
}
