/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.networking;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import org.avuna.httpd.event.base.EventDisconnected;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.util.unio.UNIOSocket;

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
	public Socket fn = null;
	public DataOutputStream fnout = null;
	public DataInputStream fnin = null;
	public int rqs = 0;
	public long rqst = 0L;
	public boolean inUse = false;
	private boolean expectingBody = false;
	private RequestPacket bodyFor = null;
	
	protected int flushPacket(byte[] packet) throws IOException {
		RequestPacket incomingRequest = null;
		if (expectingBody) {
			incomingRequest = bodyFor;
			expectingBody = false;
			bodyFor = null;
			DataInputStream pin = new DataInputStream(new ByteArrayInputStream(packet));
			incomingRequest.readBody(null, pin, host);
		}else {
			DataInputStream pin = new DataInputStream(new ByteArrayInputStream(packet));
			incomingRequest = RequestPacket.readHead(sslprep != null ? sslprep.toByteArray() : null, pin, host);
			if (sslprep != null) sslprep.reset();
			if (incomingRequest == null) {
				close();
				return -1;
			}
			String host = incomingRequest.headers.hasHeader("Host") ? incomingRequest.headers.getHeader("Host") : "";
			incomingRequest.work = this;
			incomingRequest.host = this.host.getVHost(host);
			incomingRequest.ssl = this.ssl;
			this.tos = 0;
			incomingRequest.userIP = this.s.getInetAddress().getHostAddress();
			incomingRequest.userPort = this.s.getPort();
			incomingRequest.order = this.nreqid++;
			incomingRequest.child = new ResponsePacket();
			incomingRequest.child.request = incomingRequest;
			if (this.rqst == 0L) {
				this.rqst = System.currentTimeMillis();
			}
			String cl = incomingRequest.headers.getHeader("Content-Length");
			if (cl != null) {
				expectingBody = true;
				bodyFor = incomingRequest;
				try {
					return Integer.parseInt(cl);
				}catch (NumberFormatException e) {
					close();
					return -1;
				}
			}else {
				incomingRequest.readBody(null, pin, this.host);
			}
		}
		
		this.outQueue.add(incomingRequest.child);
		this.rqs++;
		this.host.addWork(incomingRequest);
		return -1;
	}
	
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
		if (host.unio()) {
			((HTTPPacketReceiver) ((UNIOSocket) s).getCallback()).setWork(this);
		}
	}
	
	public void close() {
		host.removeWork(this);
		String ip = s.getInetAddress().getHostAddress();
		Integer cur = HostHTTP.connIPs.get(ip);
		if (cur == null) cur = 1;
		cur -= 1;
		HostHTTP.connIPs.put(ip, cur);
		host.logger.log(ip + " closed.");
		if (!s.isClosed()) try {
			s.close();
		}catch (IOException e) {
			host.logger.logError(e);
		}
		host.eventBus.callEvent(new EventDisconnected(this));
	}
}
