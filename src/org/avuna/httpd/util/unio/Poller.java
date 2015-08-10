package org.avuna.httpd.util.unio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.util.CException;
import org.avuna.httpd.util.CLib;

public class Poller {
	public Poller() {
		
	}
	
	private List<UNIOSocket> us = Collections.synchronizedList(new ArrayList<UNIOSocket>());
	
	public void addSocket(UNIOSocket s) {
		boolean n = us.size() == 0;
		us.add(s);
		if (pt != null) pt.interrupt();
		if (n) synchronized (this) {
			this.notify();
		}
	}
	
	private Thread pt = null;
	
	public void poll() throws CException {
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
		for (int i = 0; i < res.length; i++) {
			boolean close = false;
			if ((res[i] & 0x001) == 0x001) {// POLLIN
				try {
					us.get(i).read();
				}catch (IOException e) {
					close = true;
				}
			}else if ((res[i] & 0x008) == 0x008 || (res[i] & 0x020) == 0x020 || (res[i] & 0x030) == 0x030) { // POLLERR, POLLHUP, POLLNVAL
				close = true;
			}
			if (close) {
				c[i] = true;
			}
		}
		int ri = 0;
		for (int i = 0; i < c.length; i++) {
			if (c[i]) {
				try {
					us.get(ri).close();
				}catch (IOException e) {
					AvunaHTTPD.logger.logError("Failed to close socket!"); // TODO: choose better logger
					AvunaHTTPD.logger.logError(e);
				}
				us.remove(ri--);
			}
			ri++;
		}
	}
	
	public int getCount() {
		return us.size();
	}
}
