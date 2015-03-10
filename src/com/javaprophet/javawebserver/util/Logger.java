package com.javaprophet.javawebserver.util;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
	public static Logger INSTANCE;
	private PrintStream ps = null;
	private PrintWriter cache = null;
	private StringWriter cacheAccess = null;
	
	public Logger(PrintStream ps) {
		this.ps = ps;
		cacheAccess = new StringWriter();
		cache = new PrintWriter(cacheAccess);
		logThread.setDaemon(true);
		logThread.start();
	}
	
	private final static SimpleDateFormat timestamp = new SimpleDateFormat("HH:mm:ss");
	
	public static void logError(Throwable e) {
		INSTANCE.cache("[" + timestamp.format(new Date()) + "] ");
		e.printStackTrace(INSTANCE.cache);
	}
	
	public static void log(String line) {
		INSTANCE.cacheLine("[-DATE-] " + line);
	}
	
	private void cacheLine(String line) {
		cache.println(line);
	}
	
	private void cache(String str) {
		cache.print(str);
	}
	
	public final Thread logThread = new Thread("Log Flush Thread") {
		
		public void run() {
			while (true) {
				String cache = cacheAccess.toString().replace("-DATE-", timestamp.format(new Date()));
				cacheAccess.getBuffer().setLength(0);
				locallog(cache);
				try {
					Thread.sleep(1000L);
				}catch (InterruptedException e) {
					Logger.logError(e);
				}
			}
		}
	};
	
	private void locallog(String line) {
		System.out.print(line);
		ps.print(line);
	}
}
