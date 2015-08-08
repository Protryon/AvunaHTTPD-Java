/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins.base.fcgi;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import org.avuna.httpd.hosts.VHost;
import org.avuna.httpd.http.plugins.base.fcgi.FCGIConnectionManagerNMPX.AugFCGIConnection;
import org.avuna.httpd.http.plugins.base.fcgi.packets.FCGIPacket;
import org.avuna.httpd.http.plugins.base.fcgi.packets.GetValues;
import org.avuna.httpd.http.plugins.base.fcgi.packets.GetValuesResult;
import org.avuna.httpd.util.unixsocket.UnixSocket;

public class FCGIConnection extends Thread implements IFCGIManager {
	private Socket s;
	private UnixSocket us;
	private DataOutputStream out;
	private DataInputStream in;
	private final String ip;
	private final int port;
	private final boolean unix;
	protected AugFCGIConnection aug = null;
	private final VHost vhost;
	
	public FCGIConnection(VHost vhost, String ip, int port) throws IOException {
		super("FCGI Thread");
		this.ip = ip;
		this.port = port;
		s = new Socket(ip, port);
		out = new DataOutputStream(s.getOutputStream());
		out.flush();
		in = new DataInputStream(s.getInputStream());
		this.setDaemon(true);
		unix = false;
		this.vhost = vhost;
	}
	
	public FCGIConnection(VHost vhost, String unixsock) throws IOException {
		super("FCGI Thread");
		this.ip = unixsock;
		this.port = -1;
		us = new UnixSocket(unixsock);
		us.connect();
		this.unix = true;
		out = new DataOutputStream(us.getOutputStream());
		out.flush();
		in = new DataInputStream(us.getInputStream());
		this.setDaemon(true);
		this.vhost = vhost;
	}
	
	public void getSettings() {
		outQueue.add(new GetValues());
	}
	
	public boolean gotSettings = false;
	public boolean canMultiplex = false;
	
	private final HashMap<Integer, IFCGIListener> listeners = new HashMap<Integer, IFCGIListener>();
	
	protected synchronized void disassemble(IFCGIListener listener) {
		for (Integer id : listeners.keySet()) {
			if (listeners.get(id).equals(listener)) {
				listeners.remove(id);
			}
		}
		aug.taken = false;
	}
	
	private final ArrayBlockingQueue<FCGIPacket> outQueue = new ArrayBlockingQueue<FCGIPacket>(1000000);
	
	protected void write(IFCGIListener listener, FCGIPacket packet) throws IOException {
		listeners.put(packet.id, listener);
		outQueue.add(packet);
	}
	
	public boolean isClosed() {
		return unix ? us.isClosed() : s.isClosed();
	}
	
	private boolean closing = false;
	
	public void close() throws IOException {
		closing = true;
		if (unix) us.close();
		else s.close();
	}
	
	public void run() {
		try {
			while (!isClosed() && !closing) {
				boolean sleep = true;
				if (in.available() > 0) {
					sleep = false;
					FCGIPacket recv = FCGIPacket.read(in);
					if (recv.id == 0) {// management
						if (recv.type == Type.FCGI_GET_VALUES_RESULT) {
							byte[] res = ((GetValuesResult) recv).content;
							int i = 0;
							boolean gs = false;
							while (i < res.length) {
								int nl = res[i++];
								int vl = res[i++];// TODO: 4-byte?
								byte[] name = new byte[nl];
								System.arraycopy(res, i, name, 0, nl);
								i += nl;
								byte[] value = new byte[vl];
								System.arraycopy(res, i, value, 0, vl);
								i += vl;
								if (new String(name).equals("FCGI_MPXS_CONNS")) {
									canMultiplex = new String(value).equals("1");
									if (!canMultiplex && !gs) {
										gs = true;
										vhost.logger.log("[WARNING] FCGI Server does not support multiplexing, reverting to legacy systems.");
									}
								}
							}
							gotSettings = true;
						}
					}else if (listeners.containsKey(recv.id)) {
						listeners.get(recv.id).receive(recv);
					}
				}
				if (outQueue.size() > 0) {
					sleep = false;
					FCGIPacket p = outQueue.poll();
					p.write(out);
				}
				if (sleep) {
					Thread.sleep(1L);
				}
			}
		}catch (SocketException e) {
			vhost.logger.log("FCGI Connection closed!");
		}catch (Exception e) {
			vhost.logger.logError(e);
		}finally {
			if (!closing) {
				vhost.logger.log("Reconnecting!");
				try {
					if (unix) us.close();
					else s.close();
				}catch (IOException e2) {
					vhost.logger.logError(e2);
				}
				try {
					Thread.sleep(1000L);
					outQueue.clear();
					if (unix) {
						us = new UnixSocket(ip);
						out = new DataOutputStream(us.getOutputStream());
						out.flush();
						in = new DataInputStream(us.getInputStream());
					}else {
						s = new Socket(ip, port);
						out = new DataOutputStream(s.getOutputStream());
						out.flush();
						in = new DataInputStream(s.getInputStream());
					}
					run();
				}catch (Exception e) {
					vhost.logger.logError(e);
				}
			}
		}
	}
}
