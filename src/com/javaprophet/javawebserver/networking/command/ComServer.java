package com.javaprophet.javawebserver.networking.command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.util.Logger;

/**
 * Server that handles all com related stuff.
 */
public class ComServer extends Thread {

    /**
     * Our constructor
     */
	public ComServer() {
		
	}

    /**
     * Our run method that handles everything
     */
	public void run() {
		try {
            //Com config
			HashMap<String, Object> com = (HashMap<String, Object>)JavaWebServer.mainConfig.get("com");
            //Server socket and some more config options under
			ServerSocket server = new ServerSocket(Integer.parseInt((String)com.get("bindport")), 10, InetAddress.getByName((String)com.get("bindip")));
			boolean doAuth = com.get("doAuth").equals("true");
			boolean isAuth = false;
			String auth = (String)com.get("auth");
			while (!server.isClosed()) {
                //Loops for new connections
				String ip = "";
				Socket s = null;
				try {
					s = server.accept();
					DataOutputStream out = new DataOutputStream(s.getOutputStream());
					out.flush();
					DataInputStream in = new DataInputStream(s.getInputStream());
                    //Handles auth
					out.write(("Java Web Server(JWS) Version " + JavaWebServer.VERSION + JavaWebServer.crlf + (doAuth ? "Please Authenticate." + JavaWebServer.crlf : "")).getBytes());
					out.flush();
					Scanner scan = new Scanner(in);
					PrintStream ps = new PrintStream(out);
					ip = s.getInetAddress().getHostAddress();
					while (!s.isClosed()) {
						String cs = scan.nextLine();
						if (cs.equals("close")) {
							throw new IOException();
						}
						if (doAuth && !isAuth) {
							if (cs.equals(auth)) {
								isAuth = true;
								Logger.log("com[" + s.getInetAddress().getHostAddress() + "] Authenticated.");
								ps.println("Authenticated.");
							}else {
								Logger.log("com[" + s.getInetAddress().getHostAddress() + "] NOAUTH/DENIED: " + cs);
								ps.println("Please Authenticate.");
							}
						}else {
							Logger.log("com[" + s.getInetAddress().getHostAddress() + "]: " + cs);
							CommandProcessor.process(cs, ps, scan, true);
							ps.println("Command Completed.");
						}
						ps.flush();
					}
				}catch (Exception se) {
					Logger.logError(se);
					Logger.log("com[" + ip + "] Closed.");
				}finally {
					isAuth = false;
					if (s != null) {
						s.close();
					}
				}
			}
		}catch (Exception e) {
			Logger.logError(e);
		}
	}
}
