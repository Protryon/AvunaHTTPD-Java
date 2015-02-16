package com.javaprophet.javawebserver.plugins.base.fcgi;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class FCGIConnection {
	public FCGIConnection(String ip, int port) throws IOException {
		Socket s = new Socket(ip, port);
		DataOutputStream out = new DataOutputStream(s.getOutputStream());
		out.flush();
		DataInputStream in = new DataInputStream(s.getInputStream());
	}
}
