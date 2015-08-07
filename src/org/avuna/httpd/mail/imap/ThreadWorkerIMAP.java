/*
 * Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.mail.imap;

import java.io.IOException;
import java.net.SocketTimeoutException;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.hosts.ITerminatable;
import org.avuna.httpd.mail.util.StringFormatter;
import org.avuna.httpd.util.Logger;
import org.avuna.httpd.util.Stream;

public class ThreadWorkerIMAP extends Thread implements ITerminatable {
	public final HostMail host;
	
	public ThreadWorkerIMAP(HostMail host) {
		this.host = host;
	}
	
	private boolean keepRunning = true;
	
	public void terminate() {
		keepRunning = false;
		this.interrupt();
	}
	
	public static String safeRead(IMAPWork focus) throws IOException {
		String line = Stream.readLine(focus.in);
		if (focus.sslprep != null) {
			line = focus.sslprep.toString() + line;
			focus.sslprep.reset();
		}
		return line.trim();
	}
	
	public void run() {
		while (keepRunning) {
			IMAPWork focus = host.workQueueIMAP.poll();
			if (focus == null) {
				try {
					Thread.sleep(2L, 500000);
				}catch (InterruptedException e) {
					Logger.logError(e);
				}
				continue;
			}
			if (focus.s.isClosed()) {
				continue;
			}
			boolean canAdd = true;
			boolean readd = false;
			try {
				if (focus.ssl && focus.in.available() == 0) {
					focus.s.setSoTimeout(1);
					try {
						int sp = focus.in.read();
						if (sp == -1) {
							focus.s.close();
							readd = false;
							continue;
						}
						focus.sslprep.write(sp);
					}catch (SocketTimeoutException e) {
						
					}finally {
						focus.s.setSoTimeout(1000);
					}
				}
				if (focus.in.available() == 0) {
					if (focus.sns == 0L) {
						focus.sns = System.nanoTime() + 10000000000L;
						readd = true;
						if (host.workQueueIMAP.isEmpty()) {
							try {
								Thread.sleep(2L, 500000);
							}catch (InterruptedException e) {
								Logger.logError(e);
							}
						}
						continue;
					}else {
						if (focus.sns >= System.nanoTime()) {
							boolean sleep = host.workQueueIMAP.isEmpty();
							if (AvunaHTTPD.bannedIPs.contains(focus.s.getInetAddress().getHostAddress())) {
								focus.s.close();
							}else {
								readd = true;
							}
							if (sleep) {
								try {
									Thread.sleep(2L, 500000);
								}catch (InterruptedException e) {
									Logger.logError(e);
								}
							}
							continue;
						}else {
							readd = false;
							focus.s.close();
							continue;
						}
					}
				}else if (focus.in.available() > 0) {
					String line = safeRead(focus);
					focus.tos = 0;
					focus.sns = 0L;
					readd = true;
					Logger.log(focus.hashCode() + ": " + line);
					String cmd;
					String letters;
					String[] args;
					if (!(focus.state == 1)) {
						if (!line.contains(" ") || line.length() == 0) continue;
						letters = line.substring(0, line.indexOf(" "));
						line = line.substring(letters.length() + 1);
						cmd = line.substring(0, line.contains(" ") ? line.indexOf(" ") : line.length()).toLowerCase();
						line = line.substring(cmd.length()).trim();
						args = StringFormatter.congealBySurroundings(line.split(" "), "(", ")");
					}else {
						letters = line;
						cmd = "";
						args = new String[0];
					}
					boolean r = false;
					for (IMAPCommand comm : host.imaphandler.commands) {
						if ((focus.state == 1 ? comm.comm.equals("") : comm.comm.equals(cmd)) && comm.minState <= focus.state && comm.maxState >= focus.state) {
							comm.run(focus, letters, args);
							r = true;
							break;
						}
					}
					if (!r) {
						focus.writeLine(focus, letters, "BAD Command not recognized");
					}
				}
			}catch (SocketTimeoutException e) {
				focus.tos++;
				if (focus.tos < 10) {
					readd = true;
				}else {
					try {
						focus.s.close();
					}catch (IOException ex) {
						Logger.logError(ex);
					}
					readd = false;
				}
			}catch (Exception e) {
				if (!(e instanceof IOException)) Logger.logError(e);
			}finally {
				if (readd & canAdd) {
					host.workQueueIMAP.add(focus);
				}
			}
		}
	}
}
