package org.avuna.httpd.plugins.base.fcgi;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import org.avuna.httpd.plugins.base.fcgi.packets.FCGIPacket;
import org.avuna.httpd.util.Logger;

public class FCGIConnection extends Thread {
	private Socket s;
	private DataOutputStream out;
	private DataInputStream in;
	private final String ip;
	private final int port;
	
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
	
	private final HashMap<Integer, IFCGIListener> listeners = new HashMap<Integer, IFCGIListener>();
	
	protected void disassemble(IFCGIListener listener) {
		for (Integer id : listeners.keySet()) {
			if (listeners.get(id).equals(listener)) {
				listeners.remove(id);
			}
		}
	}
	
	private final ArrayBlockingQueue<FCGIPacket> outQueue = new ArrayBlockingQueue<FCGIPacket>(1000000);
	
	protected void write(IFCGIListener listener, FCGIPacket packet) throws IOException {
		listeners.put(packet.id, listener);
		outQueue.add(packet);
	}
	
	public boolean isClosed() {
		return s.isClosed();
	}
	
	public void close() throws IOException {
		s.close();
	}
	
	public void run() {
		try {
			while (!s.isClosed()) {
				boolean sleep = true;
				if (in.available() > 0) {
					sleep = false;
					FCGIPacket recv = FCGIPacket.read(in);
					if (listeners.containsKey(recv.id)) {
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
