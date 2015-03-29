package org.avuna.httpd.mail.imap;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.util.StringFormatter;
import org.avuna.httpd.util.Logger;

public class ThreadWorkerIMAP extends Thread {
	public final HostMail host;
	
	public ThreadWorkerIMAP(HostMail host) {
		this.host = host;
	}
	
	private boolean keepRunning = true;
	
	public void close() {
		keepRunning = false;
	}
	
	public void run() {
		while (keepRunning) {
			IMAPWork focus = host.workQueueIMAP.poll();
			if (focus == null) {
				try {
					Thread.sleep(1L);
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
				if (focus.in.available() == 0) {
					if (focus.sns == 0L) {
						focus.sns = System.nanoTime() + 10000000000L;
						readd = true;
						if (host.workQueueIMAP.isEmpty()) {
							try {
								Thread.sleep(1L);
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
									Thread.sleep(1L);
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
					String line = focus.in.readLine().trim();
					focus.tos = 0;
					readd = true;
					System.out.println(focus.hashCode() + ": " + line);
					String cmd;
					String letters;
					String[] args;
					if (!(focus.state == 1)) {
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
				if (!(e instanceof SocketException)) Logger.logError(e);
			}finally {
				if (readd & canAdd) {
					host.workQueueIMAP.add(focus);
				}
			}
		}
	}
}
