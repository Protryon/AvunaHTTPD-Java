/*	Avuna HTTPD - General Server Applications
    Copyright (C) 2015 Maxwell Bruce

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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
