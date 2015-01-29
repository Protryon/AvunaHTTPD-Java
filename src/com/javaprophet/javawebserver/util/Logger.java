package com.javaprophet.javawebserver.util;

import java.util.Date;
import com.javaprophet.javawebserver.networking.Connection;

public class Logger {
	public static Logger INSTANCE = new Logger();
	
	private Logger() {
		
	}
	
	public void log(String line) {
		System.out.println("[" + Connection.timestamp.format(new Date()) + "] " + line);
	}
}
