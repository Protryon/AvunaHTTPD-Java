package com.javaprophet.javawebserver.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
	public static Logger INSTANCE;
	private PrintStream ps = null;
	
	public Logger(PrintStream ps) {
		this.ps = ps;
	}
	
	public static PrintStream getStream() {
		return INSTANCE.ps;
	}
	
	public static OutputStream getOutputStream() {
		return new OutputStream() {
			boolean cr = false;
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			
			@Override
			public void write(int b) throws IOException {
				if (b == 13 && !cr) {
					cr = true;
				}else if (b == 10 && cr) {
					cr = false;
					log(bout.toString());
					bout.reset();
				}else {
					bout.write(b);
				}
			}
		};
	}
	
	private final static SimpleDateFormat timestamp = new SimpleDateFormat("HH:mm:ss");
	
	public static void logError(Throwable e) {
		log(e.toString());
		System.out.print("[" + timestamp.format(new Date()) + "] ");
		e.printStackTrace();
		INSTANCE.ps.print("[" + timestamp.format(new Date()) + "] ");
		e.printStackTrace(INSTANCE.ps);
	}
	
	public static void log(String line) {
		INSTANCE.locallog(line);
	}
	
	public void locallog(String line) {
		System.out.println("[" + timestamp.format(new Date()) + "] " + line);
		ps.println("[" + timestamp.format(new Date()) + "] " + line);
	}
}
