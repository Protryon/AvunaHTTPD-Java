/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.mail.imap;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.hosts.ITerminatable;
import org.avuna.httpd.util.unio.Poller;

public class ThreadWorkerIMAPUNIO extends ThreadWorkerIMAP implements ITerminatable {
	private static int nid = 1;
	public final Poller poller;
	public final ThreadWorkerIMAPUNIO flt;
	
	public ThreadWorkerIMAPUNIO(HostMail host) {
		super(host, "Avuna HTTPD IMAP UNIO Worker Thread #" + nid++);
		this.poller = new Poller();
		host.IMAPconns.add(this);
		this.flusher = false;
		this.flt = new ThreadWorkerIMAPUNIO(host, poller);
		this.poller.setFlushInterruptThread(this.flt);
	}
	
	public void start() {
		super.start();
		if (!flusher) {
			flt.start();
		}
	}
	
	private boolean flusher;
	
	private ThreadWorkerIMAPUNIO(HostMail host, Poller flusher) {
		super(host, "Avuna HTTPD IMAP UNIO-Flush Connection Thread #" + nid++);
		this.poller = flusher;
		this.flusher = true;
		this.flt = null;
	}
	
	public void run() {
		while (keepRunning) {
			try {
				if (flusher) {
					synchronized (this) {
						try {
							this.wait(1000);
						}catch (InterruptedException e) {
							
						}
					}
					poller.flushOut(host);
				}else poller.poll(host);
			}catch (Exception e) {
				if (!(e instanceof SocketTimeoutException)) {
					if (!(e instanceof SocketException || e instanceof StringIndexOutOfBoundsException)) host.logger.logError(e);
					continue;
				}
			}finally {
				
			}
		}
	}
	
	@Override
	public void terminate() {
		keepRunning = false;
		this.interrupt();
	}
}
