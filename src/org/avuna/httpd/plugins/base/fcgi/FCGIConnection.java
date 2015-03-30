package org.avuna.httpd.plugins.base.fcgi;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
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
		listeners.remove(listener);
	}
	
	protected void write(IFCGIListener listener, FCGIPacket packet) throws IOException {
		listeners.put(packet.id, listener);
		packet.write(out);
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
				FCGIPacket recv = FCGIPacket.read(in);
				if (listeners.containsKey(recv.id)) {
					listeners.get(recv.id).receive(recv);
				}
			}
		}catch (SocketException e) {
			Logger.log("FCGI Connection closed, reconnecting!");
		}catch (Exception e) {
			Logger.logError(e);
		}finally {
			if (s != null) try {
				s.close();
			}catch (IOException e2) {
				Logger.logError(e2);
			}
			try {
				Thread.sleep(1000L);
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
