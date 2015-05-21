package org.avuna.httpd.mail.smtp;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.hosts.ITerminatable;
import org.avuna.httpd.util.Logger;

public class ThreadWorkerSMTP extends Thread implements ITerminatable {
	private final HostMail host;
	
	public ThreadWorkerSMTP(HostMail host) {
		this.host = host;
	}
	
	private boolean keepRunning = true;
	
	public void terminate() {
		keepRunning = false;
		this.interrupt();
	}
	
	public static String safeRead(SMTPWork focus) throws IOException {
		String line = focus.in.readLine();
		if (focus.sslprep != null) {
			line = focus.sslprep.toString() + line;
			focus.sslprep.reset();
		}
		return line.trim();
	}
	
	public void run() {
		while (keepRunning) {
			SMTPWork focus = host.workQueueSMTP.poll();
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
						if (host.workQueueSMTP.isEmpty()) {
							try {
								Thread.sleep(1L);
							}catch (InterruptedException e) {
								Logger.logError(e);
							}
						}
						continue;
					}else {
						if (focus.sns >= System.nanoTime()) {
							boolean sleep = host.workQueueSMTP.isEmpty();
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
					String line = safeRead(focus);
					focus.tos = 0;
					readd = true;
					Logger.log(focus.hashCode() + ": " + line);
					String cmd = "";
					if (focus.state != 101) {
						cmd = line.contains(" ") ? line.substring(0, line.indexOf(" ")) : line;
						cmd = cmd.toLowerCase();
						line = line.substring(cmd.length()).trim();
					}
					boolean r = false;
					for (SMTPCommand comm : host.smtphandler.commands) {
						if ((focus.state == 101 || comm.comm.equals(cmd)) && focus.state <= comm.maxState && focus.state >= comm.minState) {
							comm.run(focus, line);
							r = true;
							break;
						}
					}
					if (!r) {
						focus.writeLine(500, "Command not recognized");
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
					host.workQueueSMTP.add(focus);
				}
			}
		}
	}
}
