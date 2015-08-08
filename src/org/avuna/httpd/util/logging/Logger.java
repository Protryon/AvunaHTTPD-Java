/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.util.logging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import org.avuna.httpd.hosts.Host;
import org.avuna.httpd.hosts.VHost;

public class Logger {
	public static PrintStream stdout = System.out;
	public static PrintStream stderr = System.err;
	private PrintStream ps = null, ps2 = null;
	private PrintWriter cache, cacheErr = null;
	private StringWriter cacheAccess, cacheAccessErr = null;
	private static File logs;
	
	public static void init(File logs) {
		Logger.logs = logs;
	}
	
	public Logger(VHost vh) {
		try {
			File lf = new File(logs, vh.getHostPath());
			lf.mkdirs();
			PrintStream ps = new PrintStream(new File(lf, daystamp.format(new Date()) + ".log"));
			PrintStream ps2 = new PrintStream(new File(lf, daystamp.format(new Date()) + ".errlog"));
			inite(ps, ps2);
		}catch (FileNotFoundException e) {
			e.printStackTrace(); // ???
			throw new IllegalArgumentException("Could not create log file!");
		}
	}
	
	public Logger(Host vh) {
		try {
			File lf = new File(logs, vh.getHostname());
			lf.mkdirs();
			PrintStream ps = new PrintStream(new File(lf, daystamp.format(new Date()) + ".log"));
			PrintStream ps2 = new PrintStream(new File(lf, daystamp.format(new Date()) + ".errlog"));
			inite(ps, ps2);
		}catch (FileNotFoundException e) {
			e.printStackTrace(); // ???
			throw new IllegalArgumentException("Could not create log file!");
		}
	}
	
	public Logger(String ep) {
		try {
			File lf = ep.equals("") ? logs : new File(logs, ep);
			lf.mkdirs();
			PrintStream ps = new PrintStream(new File(lf, daystamp.format(new Date()) + ".log"));
			PrintStream ps2 = new PrintStream(new File(lf, daystamp.format(new Date()) + ".errlog"));
			inite(ps, ps2);
		}catch (FileNotFoundException e) {
			e.printStackTrace(); // ???
			throw new IllegalArgumentException("Could not create log file!");
		}
	}
	
	private void inite(PrintStream ps, PrintStream ps2) {
		this.ps = ps;
		this.ps2 = ps2;
		cacheAccess = new StringWriter();
		cache = new PrintWriter(cacheAccess);
		cacheAccessErr = new StringWriter();
		cacheErr = new PrintWriter(cacheAccessErr);
		logThread.setDaemon(true);
		logThread.start();
		loggers.add(this);
	}
	
	private final static SimpleDateFormat daystamp = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
	
	private final static SimpleDateFormat timestamp = new SimpleDateFormat("HH:mm:ss");
	
	public void logError(Throwable e) {
		cacheErr.print("[-DATE-] ");
		e.printStackTrace(cacheErr);
	}
	
	public void log(String line) {
		cache.println("[-DATE-] " + line);
	}
	
	public void logError(String line) {
		cacheErr.println("[-DATE-] " + line);
	}
	
	private final Thread logThread = new Thread("Log Flush Thread") {
		
		public void run() {
			while (true) {
				if (ps != null) {
					String date = timestamp.format(new Date());
					String cache = cacheAccess.toString().replace("-DATE-", date);
					cacheAccess.getBuffer().setLength(0);
					stdout.print(cache);
					ps.print(cache);
					String cacheErr = cacheAccessErr.toString().replace("-DATE-", date);
					cacheAccessErr.getBuffer().setLength(0);
					stderr.print(cacheErr);
					ps2.print(cacheErr);
				}
				try {
					Thread.sleep(1000L);
				}catch (InterruptedException e) {
					// Logger.logError(e);
				}
			}
		}
	};
	
	public void flush() {
		logThread.interrupt();
	}
	
	private static ArrayList<Logger> loggers = new ArrayList<Logger>();
	
	public static void flushAll() {
		for (Logger l : loggers) {
			l.flush();
		}
	}
}