/*
 * Avuna HTTPD - General Server Applications
 * Copyright (C) 2015 Maxwell Bruce
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.avuna.httpd.dns;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import org.avuna.httpd.hosts.HostDNS;
import org.avuna.httpd.hosts.ITerminatable;
import org.avuna.httpd.util.Logger;

/**
 * Created by JavaProphet on 8/13/14 at 10:56 PM.
 */
public class TCPServer extends Thread implements IServer, ITerminatable {
	private final ServerSocket server;
	private final HostDNS host;
	
	public TCPServer(HostDNS host, ServerSocket server) {
		super("DNS TCPServer");
		this.server = server;
		this.host = host;
	}
	
	public void run() {
		try {
			while (!server.isClosed()) {
				try {
					final Socket s = server.accept(); // TODO: thread
					s.setTcpNoDelay(true);
					s.setSoTimeout(1000);
					final DataOutputStream out = new DataOutputStream(s.getOutputStream());
					out.flush();
					final DataInputStream in = new DataInputStream(s.getInputStream());
					host.addWork(new WorkTCP(s, in, out));
				}catch (SocketException e2) {
				}catch (Exception e) {
					Logger.logError(e);
				}
			}
		}catch (Exception e) {
			Logger.logError(e);
		}finally {
			try {
				server.close();
			}catch (IOException e) {
				Logger.logError(e);
			}
		}
	}
	
	@Override
	public void terminate() {
		try {
			server.close();
		}catch (IOException e) {
			Logger.logError(e);
		}
	}
}
