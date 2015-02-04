package com.javaprophet.javawebserver.networking.command;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class ComClient {
	public static void run(String ip, int port) {
		Thread thr = null;
		try {
			final Socket s = new Socket(ip, port);
			PrintStream out = new PrintStream(s.getOutputStream());
			out.flush();
			final Scanner scan = new Scanner(s.getInputStream());
			Scanner inp = new Scanner(System.in);
			final Thread mt = Thread.currentThread();
			thr = new Thread() {
				public void run() {
					while (!s.isClosed()) {
						String com = scan.nextLine();
						System.out.println(com);
						System.out.flush();
					}
					mt.interrupt();
				}
			};
			thr.start();
			while (!s.isClosed()) {
				String com = inp.nextLine();
				out.println(com);
				out.flush();
				if (com.equals("close")) {
					s.close();
					System.exit(0);
				}
			}
		}catch (IOException e) {
			System.out.println("Connection Terminated.");
		}
		if (thr != null) {
			thr.interrupt();
		}
	}
}
