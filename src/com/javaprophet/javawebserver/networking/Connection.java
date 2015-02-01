package com.javaprophet.javawebserver.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.util.Logger;

/**
 * Handles a single connection.
 */
public class Connection {
	private Socket s = null;
	private DataInputStream in = null;
	private DataOutputStream out = null;
	private boolean ssl = false;
	private static ArrayList<ThreadWorker> workers = new ArrayList<ThreadWorker>();
	
	public static void init() {
		for (int i = 0; i < Integer.parseInt((String)JavaWebServer.mainConfig.get("nginxThreadCount")); i++) {
			ThreadWorker worker = new ThreadWorker();
			workers.add(worker);
			worker.start();
		}
	}
	
	public Connection(Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
		this.s = s;
		this.in = in;
		this.out = out;
		this.ssl = ssl;
		Logger.INSTANCE.log(s.getInetAddress().getHostAddress() + " connected.");
		try {
			s.setSoTimeout(1000);
		}catch (SocketException e1) {
			e1.printStackTrace();
		}
	}
	
	public void handleConnection() {
		ThreadWorker.addWork(s, in, out, ssl);
		JavaWebServer.runningThreads.remove(this);
	}
}
