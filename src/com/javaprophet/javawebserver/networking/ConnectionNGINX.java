package com.javaprophet.javawebserver.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import com.javaprophet.javawebserver.JavaWebServer;

/**
 * Handles a single connection.
 */
public class ConnectionNGINX extends Connection {
	
	private static ArrayList<ThreadNGINXWorker> workers = new ArrayList<ThreadNGINXWorker>();
	
	public static void init() {
		for (int i = 0; i < Integer.parseInt((String)JavaWebServer.mainConfig.get("nginxThreadCount")); i++) {
			ThreadNGINXWorker worker = new ThreadNGINXWorker();
			workers.add(worker);
			worker.start();
		}
	}
	
	public ConnectionNGINX(Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
		super(s, in, out, ssl);
		try {
			s.setSoTimeout(1000);
		}catch (SocketException e1) {
			e1.printStackTrace();
		}
	}
	
	public void handleConnection() {
		ThreadNGINXWorker.addWork(s, in, out, ssl);
		JavaWebServer.runningThreads.remove(this);
	}
}
