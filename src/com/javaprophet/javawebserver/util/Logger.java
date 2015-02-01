package com.javaprophet.javawebserver.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
	public static Logger INSTANCE = new Logger();
	
	private Logger() {
		
	}
	
	private final static SimpleDateFormat timestamp = new SimpleDateFormat("HH:mm:ss");
	
	public void log(String line) {
		System.out.println("[" + timestamp.format(new Date()) + "] " + line);
	}
}
