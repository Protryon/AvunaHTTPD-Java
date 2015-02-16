package com.javaprophet.javawebserver.plugins.base.fcgi;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
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
	
	public void run() {
		try {
			while (!s.isClosed()) {
				try {
					
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
