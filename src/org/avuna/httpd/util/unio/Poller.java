/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */
package org.avuna.httpd.util.unio;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.Host;
import org.avuna.httpd.util.CException;
import org.avuna.httpd.util.CLib;

public class Poller {
	public Poller() {
	
	}
	
	private List<UNIOSocket> us = Collections.synchronizedList(new ArrayList<UNIOSocket>());
	private Thread flushInterrupt = null;
	
	public void setFlushInterruptThread(Thread t) {
		flushInterrupt = t;
	}
	
	public void addSocket(UNIOSocket s) {
		boolean n = us.size() == 0;
		s.setFlushInterruptThread(flushInterrupt);
		us.add(s);
		if (pt != null) pt.interrupt();
		if (n) synchronized (this) {
			this.notify();
		}
	}
	
	public void flushOut(Host host) throws IOException {
		if (us.size() == 0) return;
		int[] pfd;
		synchronized (us) {
			pfd = new int[us.size()];
			for (int i = 0; i < us.size(); i++) {
				pfd[i] = us.get(i).sockfd;
			}
			// other than having new sockets added, nothing SHOULD change our list.
			// read data, close sockets, etc.
			long t = System.currentTimeMillis();
			for (int i = 0; i < us.size(); i++) {
				UNIOSocket uss = us.get(i);
				boolean close = false;
				long to = uss.getTimeout();
				if (to > 0L && (uss.lr) + to < t && !uss.getHoldTimeout()) {
					close = true;
				}
				if (!close) try {
					uss.write();
				}catch (IOException e) {
					if (!(e instanceof SocketException)) host.logger.logError(e);
					close = true;
				}
				if (close) {
					uss.to = true;
					uss.close();
				}
			}
		}
	}
	
	private Thread pt = null;
	
	public void poll(Host host) throws CException {
		if (us.size() == 0) {
			synchronized (this) {
				try {
					this.wait();
				}catch (InterruptedException e) {}
			}
		}
		int[] pfd;
		synchronized (us) {
			pfd = new int[us.size()];
			for (int i = 0; i < us.size(); i++) {
				pfd[i] = us.get(i).sockfd;
			}
		}
		// other than having new sockets added, nothing SHOULD change our list.
		pt = Thread.currentThread();
		int[] res = CLib.poll(pfd);
		pt = null;
		if (res == null) {
			int e = CLib.errno();
			if (e == 4) return; // interrupted
			throw new CException(e, "Unix poll() failed!");
		}
		boolean[] c = new boolean[res.length];
		// read data, close sockets, etc.
		synchronized (us) {
			long t = System.currentTimeMillis();
			for (int i = 0; i < res.length; i++) {
				UNIOSocket uss = us.get(i);
				boolean close = false;
				long to = uss.getTimeout();
				try {
					if ((to > 0L && uss.lr + to < t && !uss.getHoldTimeout())) {
						close = true;
					}
					if (uss.stlsi) {
						continue;
					}
					if (!close && (res[i] & 0x001) == 0x001) {// POLLIN
						try {
							if (uss.read() == 0) {
								close = true; // EOF
							}
						}catch (IOException e) {
							if (!(e instanceof SocketException)) host.logger.logError(e);
							close = true;
						}
					}
					if (!close && (res[i] & 0x008) == 0x008 || (res[i] & 0x020) == 0x020 || (res[i] & 0x030) == 0x030) { // POLLERR, POLLHUP, POLLNVAL
						close = true;
					}
				}finally {
					if (close) {
						c[i] = true;
					}
				}
			}
			int ri = 0;
			for (int i = 0; i < c.length; i++) {
				if (c[i]) {
					try {
						UNIOSocket uss = us.get(ri);
						uss.to = true;
						uss.close();
					}catch (IOException e) {
						AvunaHTTPD.logger.logError("Failed to close socket!"); // TODO: choose better logger
						AvunaHTTPD.logger.logError(e);
					}
					us.remove(ri--);
				}
				ri++;
			}
		}
	}
	
	public int getCount() {
		return us.size();
	}
}
