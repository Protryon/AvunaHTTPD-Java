package org.avuna.httpd.plugins.base.fcgi;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import org.avuna.httpd.plugins.base.fcgi.FCGIConnectionManagerNMPX.AugFCGIConnection;
import org.avuna.httpd.plugins.base.fcgi.packets.FCGIPacket;
import org.avuna.httpd.plugins.base.fcgi.packets.GetValues;
import org.avuna.httpd.plugins.base.fcgi.packets.GetValuesResult;
import org.avuna.httpd.util.Logger;

public class FCGIConnection extends Thread implements IFCGIManager {
	private Socket s;
	private DataOutputStream out;
	private DataInputStream in;
	private final String ip;
	private final int port;
	protected AugFCGIConnection aug = null;
	
	public FCGIConnection(String ip, int port) throws IOException {
		super("FCGI Thread");
		this.ip = ip;
		this.port = port;
		s = new Socket(ip, port);
		out = new DataOutputStream(s.getOutputStream());
		out.flush();
		in = new DataInputStream(s.getInputStream());
		this.setDaemon(true);
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
		return s.isClosed();
	}
	
	private boolean closing = false;
	
	public void close() throws IOException {
		closing = true;
		s.close();
	}
	
	public void run() {
		try {
			while (!s.isClosed() && !closing) {
				boolean sleep = true;
				System.out.println(in.available());
				if (in.available() > 0) {
					sleep = false;
					FCGIPacket recv = FCGIPacket.read(in);
					if (recv.id == 0) {// management
						if (recv.type == Type.FCGI_GET_VALUES_RESULT) {
							byte[] res = ((GetValuesResult)recv).content;
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
										Logger.log("[WARNING] FCGI Server does not support multiplexing, reverting to legacy systems.");
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
			Logger.log("FCGI Connection closed!");
		}catch (Exception e) {
			Logger.logError(e);
		}finally {
			if (!closing) {
				Logger.log("Reconnecting!");
				if (s != null) try {
					s.close();
				}catch (IOException e2) {
					Logger.logError(e2);
				}
				try {
					Thread.sleep(1000L);
					outQueue.clear();
					s = new Socket(ip, port);
					out = new DataOutputStream(s.getOutputStream());
					out.flush();
					in = new DataInputStream(s.getInputStream());
					run();
				}catch (Exception e) {
					Logger.logError(e);
				}
			}
		}
	}
}
