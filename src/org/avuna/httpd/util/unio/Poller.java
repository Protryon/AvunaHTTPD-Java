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
		us.add(s);
	}
	
	public void poll() throws CException {
		int[] pfd;
		synchronized (us) {
			pfd = new int[us.size()];
			for (int i = 0; i < us.size(); i++) {
				pfd[i] = us.get(i).sockfd;
			}
		}
		// other than having new sockets added, nothing SHOULD change our list.
		int[] res = CLib.poll(pfd);
		if (res == null) {
			throw new CException(CLib.errno(), "Unix poll() failed!");
		}
		// read data, close sockets, etc.
		int d = 0;
		for (int i = 0; i < res.length; i++) {
			boolean close = false;
			if ((res[i + d] & 0x001) == 0x001) {// POLLIN
				try {
					us.get(i).read();
				}catch (IOException e) {
					close = true;
				}
			}else if ((res[i + d] & 0x008) == 0x008 || (res[i + d] & 0x020) == 0x020 || (res[i + d] & 0x030) == 0x030) { // POLLERR, POLLHUP, POLLNVAL
				close = true;
			}
			if (close) {
				try {
					us.get(i).close();
				}catch (IOException e) {
					AvunaHTTPD.logger.logError("Failed to close socket!");
					AvunaHTTPD.logger.logError(e);
				}
				d++;
				us.remove(i--);
			}
		}
	}
	
	public int getCount() {
		return us.size();
	}
}
