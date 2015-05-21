package org.avuna.httpd.util;

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
		if (INSTANCE == null) {
			System.out.println("[" + timestamp.format(new Date()) + "] ");
			e.printStackTrace();
			return;
		}
		INSTANCE.cache("[-DATE-] ");
		e.printStackTrace(INSTANCE.cache);
	}
	
	public static void log(String line) {
		if (INSTANCE == null) {
			System.out.println(line);
		}else INSTANCE.cacheLine("[-DATE-] " + line);
	}
	
	private void cacheLine(String line) {
		cache.println(line);
	}
	
	private void cache(String str) {
		cache.print(str);
	}
	
	private final Thread logThread = new Thread("Log Flush Thread") {
		
		public void run() {
			while (true) {
				if (ps != null) {
					String cache = cacheAccess.toString().replace("-DATE-", timestamp.format(new Date()));
					cacheAccess.getBuffer().setLength(0);
					System.out.print(cache);
					ps.print(cache);
				}
				try {
					Thread.sleep(1000L);
				}catch (InterruptedException e) {
					// Logger.logError(e);
				}
			}
		}
	};
	
	public static void flush() {
		INSTANCE.logThread.interrupt();
	}
}
