package org.avuna.httpd.com;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Scanner;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.util.Logger;

/**
 * Server that handles all com related stuff.
 */
public class ComServer extends Thread {
	
	private final String[] auth;
	private final ServerSocket server;
	
	/**
	 * Our constructor
	 */
	public ComServer(ServerSocket server, String[] auth) {
		super("ComServer");
		this.auth = auth;
		this.server = server;
		this.setDaemon(true);
	}
	
	private HashMap<String, Integer> attempts = new HashMap<String, Integer>();
	
	/**
	 * Our run method that handles everything
	 */
	public void run() {
		try {
			// Com config
			// Server socket and some more config options under
			boolean doAuth = auth != null;
			boolean isAuth = false;
			while (!server.isClosed()) {
				// Loops for new connections
				String ip = "";
				Socket s = null;
				try {
					s = server.accept();
					if (attempts.containsKey(s.getInetAddress().getHostAddress())) {
						if (attempts.get(s.getInetAddress().getHostAddress()) >= 4) {
							s.close();
							continue;
						}
					}
					DataOutputStream out = new DataOutputStream(s.getOutputStream());
					out.flush();
					DataInputStream in = new DataInputStream(s.getInputStream());
					// Handles auth
					out.write(("Avuna HTTPD Version " + AvunaHTTPD.VERSION + AvunaHTTPD.crlf + (doAuth ? "Username: " : "")).getBytes());
					out.flush();
					Scanner scan = new Scanner(in);
					PrintStream ps = new PrintStream(out);
					ip = s.getInetAddress().getHostAddress();
					String user = "";
					while (!s.isClosed()) {
						String cs = scan.nextLine().trim();
						if (cs.equals("close")) {
							throw new IOException();
						}
						if (doAuth && !isAuth) {
							if (cs.length() > 0 && user.length() == 0) {
								user = cs;
								ps.print("Password: ");
							}else if (user.length() == 0) {
								Logger.log("com[" + s.getInetAddress().getHostAddress() + "] NOAUTH/DENIED: " + cs);
								ps.print("Username: ");
							}else if (user.length() > 0) {
								String total = user + ":" + cs;
								for (String pa : auth) {
									if (pa.equals(total)) {
										isAuth = true;
										Logger.log("com[" + s.getInetAddress().getHostAddress() + "] Authenticated.");
										ps.println("Authenticated.");
										break;
									}
								}
								if (!isAuth) {
									Logger.log("com[" + s.getInetAddress().getHostAddress() + "] NOAUTH/DENIED: " + user);
									ps.println("Invalid Credentials!");
									if (!attempts.containsKey(s.getInetAddress().getHostAddress())) {
										attempts.put(s.getInetAddress().getHostAddress(), 0);
									}
									int a = attempts.get(s.getInetAddress().getHostAddress());
									attempts.put(s.getInetAddress().getHostAddress(), a + 1);
									if (a >= 4) {
										ps.println("More than 5 invalid attempts, you are banned until server restart.");
										s.close();
										Logger.log("com[" + s.getInetAddress().getHostAddress() + "] KILLED 5 INVALID ATTEMPTS: " + user);
									}
									user = "";
									ps.print("Username: ");
								}
							}else {
								s.close();
								Logger.log("com[" + s.getInetAddress().getHostAddress() + "] <FISHY ERROR> CLOSED: " + (user.length() == 0 ? cs : user));
							}
						}else {
							Logger.log("com[" + s.getInetAddress().getHostAddress() + "]: " + cs);
							CommandProcessor.process(cs, ps, scan);
							ps.println("Command Completed.");
						}
						ps.flush();
					}
				}catch (Exception se) {
					if (!(se instanceof NoSuchElementException || se instanceof SocketException)) Logger.logError(se);
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
		}finally {
			if (server != null) try {
				server.close();
			}catch (IOException e) {
				Logger.logError(e);
			}
		}
	}
}
