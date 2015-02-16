package com.javaprophet.javawebserver.plugins.base.fcgi;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import com.javaprophet.javawebserver.plugins.base.fcgi.packets.FCGIPacket;
import com.javaprophet.javawebserver.util.Logger;

public class FCGIConnection extends Thread {
	private final Socket s;
	private final DataOutputStream out;
	private final DataInputStream in;
	
	public FCGIConnection(String ip, int port) throws IOException {
		s = new Socket(ip, port);
		out = new DataOutputStream(s.getOutputStream());
		out.flush();
		in = new DataInputStream(s.getInputStream());
	}
	
	private final HashMap<Integer, IFCGIListener> listeners = new HashMap<Integer, IFCGIListener>();
	
	protected void disassemble(IFCGIListener listener) {
		listeners.remove(listener);
	}
	
	protected void write(IFCGIListener listener, FCGIPacket packet) throws IOException {
		listeners.put(packet.id, listener);
		packet.write(out);
	}
	
	public void run() {
		try {
			while (!s.isClosed()) {
				try {
					FCGIPacket recv = FCGIPacket.read(in);
					if (listeners.containsKey(recv.id)) {
						listeners.get(recv.id).receive(recv);
					}
				}catch (Exception e) {
					Logger.logError(e);
				}
			}
		}finally {
			if (s != null) try {
				s.close();
			}catch (IOException e) {
				Logger.logError(e);
			}
		}
	}
}
