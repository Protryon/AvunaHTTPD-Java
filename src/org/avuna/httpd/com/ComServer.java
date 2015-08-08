/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

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
import org.avuna.httpd.hosts.HostCom;

/** Server that handles all com related stuff. */
public class ComServer extends Thread {
	
	private final String[] auth;
	private final ServerSocket server;
	private final HostCom host;
	
	/** Our constructor */
	public ComServer(HostCom host, ServerSocket server, String[] auth) {
		super("ComServer");
		this.host = host;
		this.auth = auth;
		this.server = server;
		this.setDaemon(true);
	}
	
	private HashMap<String, Integer> attempts = new HashMap<String, Integer>();
	
	/** Our run method that handles everything */
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
					s.setTcpNoDelay(true);
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
					CommandContext context = AvunaHTTPD.commandRegistry.newContext(ps, scan);
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
								host.logger.log("com[" + s.getInetAddress().getHostAddress() + "] NOAUTH/DENIED: " + cs);
								ps.print("Username: ");
							}else if (user.length() > 0) {
								String total = user + ":" + cs;
								for (String pa : auth) {
									if (pa.equals(total)) {
										isAuth = true;
										host.logger.log("com[" + s.getInetAddress().getHostAddress() + "] Authenticated.");
										ps.println("Authenticated.");
										break;
									}
								}
								if (!isAuth) {
									host.logger.log("com[" + s.getInetAddress().getHostAddress() + "] NOAUTH/DENIED: " + user);
									ps.println("Invalid Credentials!");
									if (!attempts.containsKey(s.getInetAddress().getHostAddress())) {
										attempts.put(s.getInetAddress().getHostAddress(), 0);
									}
									int a = attempts.get(s.getInetAddress().getHostAddress());
									attempts.put(s.getInetAddress().getHostAddress(), a + 1);
									if (a >= 4) {
										ps.println("More than 5 invalid attempts, you are banned until server restart.");
										s.close();
										host.logger.log("com[" + s.getInetAddress().getHostAddress() + "] KILLED 5 INVALID ATTEMPTS: " + user);
									}
									user = "";
									ps.print("Username: ");
								}
							}else {
								s.close();
								host.logger.log("com[" + s.getInetAddress().getHostAddress() + "] <FISHY ERROR> CLOSED: " + (user.length() == 0 ? cs : user));
							}
						}else {
							host.logger.log("com[" + s.getInetAddress().getHostAddress() + "]: " + cs);
							AvunaHTTPD.commandRegistry.processCommand(cs, context);
							ps.println("Command Completed.");
						}
						ps.flush();
					}
				}catch (Exception se) {
					if (!(se instanceof NoSuchElementException || se instanceof SocketException)) host.logger.logError(se);
					host.logger.log("com[" + ip + "] Closed.");
				}finally {
					isAuth = false;
					if (s != null) {
						s.close();
					}
				}
			}
		}catch (Exception e) {
			host.logger.logError(e);
		}finally {
			if (server != null) try {
				server.close();
			}catch (IOException e) {
				host.logger.logError(e);
			}
		}
	}
}
