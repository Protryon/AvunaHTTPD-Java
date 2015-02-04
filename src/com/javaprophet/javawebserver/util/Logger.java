package com.javaprophet.javawebserver.util;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger extends PrintStream {
	public static Logger INSTANCE;
	private PrintStream ps = null;
	
	public Logger(PrintStream ps) {
		super(ps);
		this.ps = ps;
	}
	
	public void write(int i) {
		ps.write(i);
		System.out.write(i);
	}
	
	public static PrintStream getStream() {
		return INSTANCE.ps;
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
