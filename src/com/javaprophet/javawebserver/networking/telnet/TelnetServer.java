package com.javaprophet.javawebserver.networking.telnet;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import com.javaprophet.javawebserver.CommandProcessor;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.util.Logger;

public class TelnetServer extends Thread {
	public TelnetServer() {
		
	}
	
	public void run() {
		try {
			HashMap<String, Object> telnet = (HashMap<String, Object>)JavaWebServer.mainConfig.get("telnet");
			ServerSocket server = new ServerSocket(Integer.parseInt((String)telnet.get("bindport")), 10, InetAddress.getByName((String)telnet.get("bindip")));
			boolean doAuth = telnet.get("doAuth").equals("true");
			boolean isAuth = false;
			String auth = (String)telnet.get("auth");
			while (!server.isClosed()) {
				try {
					Socket s = server.accept();
					DataOutputStream out = new DataOutputStream(s.getOutputStream());
					out.flush();
					DataInputStream in = new DataInputStream(s.getInputStream());
					out.write(255);
					out.write(251);
					out.write(1);
					out.write(255);
					out.write(254);
					out.write(1);
					out.write(255);
					out.write(251);
					out.write(3);
					out.write(255);
					out.write(253);
					out.write(3);
					out.write(("Java Web Server(JWS) Version " + JavaWebServer.VERSION + JavaWebServer.crlf + (doAuth ? "Please Authenticate." + JavaWebServer.crlf : "")).getBytes());
					out.flush();
					ByteArrayOutputStream input = new ByteArrayOutputStream();
					boolean cr = false;
					while (!s.isClosed()) {
						int b = in.read();
						if (b == 255) {
							int clause = in.read();
							int effect = in.read();
							// ?
						}else if (b == 244) {
							s.close();
							continue;
						}else if (b == 238) {
							s.close();
							continue;
						}else if (b == 246) {
							out.write(("We are here." + JavaWebServer.crlf).getBytes());
							out.flush();
						}else if (b == 247) {
							byte[] t = input.toByteArray();
							input.reset();
							input.write(t, 0, t.length - 1);
						}else if (b == 248) {
							input.reset();
						}else if (b == 242) {
							// not a problem
						}else {
							if (b == 13 && !cr) {
								cr = true;
							}else if (b == 10 && cr) {
								cr = false;
								String com = input.toString();
								input.reset();
								if (doAuth && !isAuth) {
									if (com.equals(auth)) {
										isAuth = true;
										String res = com + JavaWebServer.crlf + "Authenticated." + JavaWebServer.crlf;
										Logger.log("telnet[" + s.getInetAddress().getHostAddress() + "] Authenticated.");
										out.write(res.getBytes());
									}else {
										Logger.log("telnet[" + s.getInetAddress().getHostAddress() + "] NOAUTH/DENIED: " + com);
										String res = com + JavaWebServer.crlf + "Please Authenticate." + JavaWebServer.crlf;
										out.write(res.getBytes());
									}
								}else {
									Logger.log("telnet[" + s.getInetAddress().getHostAddress() + "]: " + com);
									String res = com + JavaWebServer.crlf;
									out.write(res.getBytes());
									CommandProcessor.process(com, out, in, true);
									out.write(("Command Completed." + JavaWebServer.crlf).getBytes());
								}
								out.flush();
							}else {
								input.write(b);
							}
						}
						out.write(b);
						out.flush();
					}
				}catch (IOException se) {
					Logger.logError(se);
				}
			}
		}catch (Exception e) {
			Logger.logError(e);
		}
	}
}
