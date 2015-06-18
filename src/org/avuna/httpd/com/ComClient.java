/*
 * Avuna HTTPD - General Server Applications
 * Copyright (C) 2015 Maxwell Bruce
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.avuna.httpd.com;

import java.io.DataInputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;
import org.avuna.httpd.util.unixsocket.UnixSocket;

/**
 * Command Client that is connected to ComServer to handle commands from the outside.
 */
public class ComClient {
	
	/**
	 * Our socket connection
	 */
	static Socket cs = null;
	
	/**
	 * Our inputstream to read from the socket connection
	 */
	static DataInputStream scan = null;
	
	/**
	 * Out writer to write to the ComClient
	 */
	static PrintStream out = null;
	
	/**
	 * Runs the ComClient
	 * 
	 * @param ip the ip
	 * @param port the port to use
	 */
	public static void run(String ip, int port) {
		// Handles console/com input and processes.
		Thread mte = new Thread() {
			public void run() {
				@SuppressWarnings("resource")
				Scanner inp = new Scanner(System.in);
				while (inp.hasNextLine()) {
					String com = inp.nextLine();
					if (com.equals("close")) {
						System.exit(0);
					}
					if (cs != null && out != null && !cs.isClosed()) {
						out.println(com);
						out.flush();
					}
				}
			}
		};
		// Start the thread looping
		mte.start();
		
		while (true) {
			try {
				cs = new Socket(ip, port);
				cs.setTcpNoDelay(true);
				out = new PrintStream(cs.getOutputStream());
				out.flush();
				scan = new DataInputStream(cs.getInputStream());
				while (!cs.isClosed()) {
					int c = scan.read();
					if (c == -1) {
						cs.close();
						throw new Exception();
					}
					System.out.append((char)c);
				}
			}catch (Exception e) {
				System.out.println("Connection Terminated, restarting... [close]");
			}
			try {
				Thread.sleep(1000L);
			}catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void runUnix(String file) {
		// Handles console/com input and processes.
		Thread mte = new Thread() {
			public void run() {
				@SuppressWarnings("resource")
				Scanner inp = new Scanner(System.in);
				while (inp.hasNextLine()) {
					String com = inp.nextLine();
					if (com.equals("close")) {
						System.exit(0);
					}
					if (cs != null && out != null && !cs.isClosed()) {
						out.println(com);
						out.flush();
					}
				}
			}
		};
		// Start the thread looping
		mte.start();
		
		while (true) {
			try {
				cs = new UnixSocket(file);
				out = new PrintStream(cs.getOutputStream());
				out.flush();
				scan = new DataInputStream(cs.getInputStream());
				while (!cs.isClosed()) {
					int c = scan.read();
					if (c == -1) {
						cs.close();
						throw new Exception();
					}
					System.out.append((char)c);
				}
			}catch (Exception e) {
				System.out.println("Connection Terminated, restarting... [close]");
			}
			try {
				Thread.sleep(1000L);
			}catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
