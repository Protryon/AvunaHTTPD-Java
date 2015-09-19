/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.mail.smtp;

import java.io.IOException;
import java.net.SocketTimeoutException;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.hosts.ITerminatable;
import org.avuna.httpd.util.Stream;

public class ThreadWorkerSMTP extends Thread implements ITerminatable {
	public final HostMail host;
	
	public ThreadWorkerSMTP(HostMail host) {
		this.host = host;
	}
	
	protected ThreadWorkerSMTP(HostMail host, String string) {
		super(string);
		this.host = host;
	}
	
	protected boolean keepRunning = true;
	
	public void terminate() {
		keepRunning = false;
		this.interrupt();
	}
	
	public static String safeRead(SMTPWork focus) throws IOException {
		String line = Stream.readLine(focus.in);
		if (focus.sslprep != null) {
			line = focus.sslprep.toString() + line;
			focus.sslprep.reset();
		}
		return line;
	}
	
	public void run() {
		while (keepRunning) {
			SMTPWork focus = host.getSMTPWork();
			if (focus == null) {
				try {
					Thread.sleep(2L, 500000);
				}catch (InterruptedException e) {}
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
						if (host.SMTPworks.size() == 0) {
							try {
								Thread.sleep(2L, 500000);
							}catch (InterruptedException e) {
								host.logger.logError(e);
							}
						}
						continue;
					}else {
						if (focus.sns >= System.nanoTime()) {
							boolean sleep = host.SMTPworks.size() == 0;
							if (AvunaHTTPD.bannedIPs.contains(focus.s.getInetAddress().getHostAddress())) {
								focus.s.close();
							}else {
								readd = true;
							}
							if (sleep) {
								try {
									Thread.sleep(2L, 500000);
								}catch (InterruptedException e) {
									host.logger.logError(e);
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
					focus.readLine(line);
				}
			}catch (SocketTimeoutException e) {
				focus.tos++;
				if (focus.tos < 10) {
					readd = true;
				}else {
					try {
						focus.s.close();
					}catch (IOException ex) {
						host.logger.logError(ex);
					}
					readd = false;
				}
			}catch (Exception e) {
				if (!(e instanceof IOException)) host.logger.logError(e);
			}finally {
				if (readd & canAdd) {
					if (!readd || !canAdd) {
						try {
							focus.close();
						}catch (IOException e) {
							host.logger.logError(e);
						}
					}else {
						focus.inUse = false;
					}
				}
			}
		}
	}
}
