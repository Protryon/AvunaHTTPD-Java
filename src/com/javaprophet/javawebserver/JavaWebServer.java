package com.javaprophet.javawebserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class JavaWebServer {
	public static final String VERSION = "1.0";
	public static Config mainConfig;
	public static ArrayList<Connection> runningThreads = new ArrayList<Connection>();
	
	public static void main(String[] args) {
		try {
			System.out.println("Loading Configs");
			mainConfig = new Config(new File("C:\\temp.cfg"));
			mainConfig.load();
			System.out.println("Starting Server");
			ServerSocket server = new ServerSocket((int)((long)mainConfig.get("bindport")));
			while (!server.isClosed()) {
				Socket s = server.accept();
				DataOutputStream out = new DataOutputStream(s.getOutputStream());
				out.flush();
				DataInputStream in = new DataInputStream(s.getInputStream());
				Connection c = new Connection(s, in, out);
				c.start();
				runningThreads.add(c);
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		if (mainConfig != null) {
			try {
				mainConfig.save();
			}catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
