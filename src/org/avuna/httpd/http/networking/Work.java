/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.networking;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import org.avuna.httpd.event.base.EventDisconnected;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.http.networking.httpm.MasterConn;
import org.avuna.httpd.util.Logger;

public class Work {
	public final Socket s;
	public final DataInputStream in;
	public final DataOutputStream out;
	public final boolean ssl;
	public final HostHTTP host;
	public int tos = 0;
	public long sns = 0L;
	public int nreqid = 1;
	public ByteArrayOutputStream sslprep = null;
	public ArrayBlockingQueue<ResponsePacket> outQueue = new ArrayBlockingQueue<ResponsePacket>(64);
	public boolean blockTimeout = false;
	public MasterConn cn = null;
	public int rqs = 0;
	public long rqst = 0L;
	public boolean inUse = false;
	
	// public ResponsePacket[] pipeline = new ResponsePacket[32];
	
	public Work(HostHTTP host, Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
		this.host = host;
		this.s = s;
		this.in = in;
		this.out = out;
		this.ssl = ssl;
		if (ssl) {
			sslprep = new ByteArrayOutputStream();
		}
	}
	
	public void close() {
		String ip = s.getInetAddress().getHostAddress();
		Integer cur = HostHTTP.connIPs.get(ip);
		if (cur == null) cur = 1;
		cur -= 1;
		HostHTTP.connIPs.put(ip, cur);
		Logger.log(ip + " closed.");
		try {
			s.close();
		}catch (IOException e) {
			Logger.logError(e);
		}
		host.eventBus.callEvent(new EventDisconnected(this));
	}
}
