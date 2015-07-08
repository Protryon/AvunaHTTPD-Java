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

package org.avuna.httpd.http.networking;

import java.io.IOException;
import java.net.SocketException;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.http.plugins.avunaagent.AvunaAgentStream;
import org.avuna.httpd.util.Logger;

public class ThreadJavaLoaderStreamWorker extends Thread {
	private final Work work;
	private final RequestPacket req;
	private final ResponsePacket resp;
	private final AvunaAgentStream reqStream;
	private final HostHTTP host;
	
	public ThreadJavaLoaderStreamWorker(HostHTTP host, Work work, RequestPacket req, ResponsePacket resp, AvunaAgentStream reqStream) {
		this.work = work;
		this.req = req;
		this.resp = resp;
		this.reqStream = reqStream;
		this.host = host;
		this.setDaemon(true);
	}
	
	public void run() {
		try {
			ChunkedOutputStream cos = new ChunkedOutputStream(work.out, resp, resp.headers.hasHeader("Content-Encoding") && resp.headers.getHeader("Content-Encoding").contains("gzip"));
			reqStream.generate(cos, req, resp);
			host.readdWork(work);
		}catch (IOException e) {
			if (!(e instanceof SocketException)) Logger.logError(e);
		}finally {
			String ip = work.s.getInetAddress().getHostAddress();
			Integer cur = HostHTTP.connIPs.get(ip);
			if (cur == null) cur = 1;
			cur -= 1;
			HostHTTP.connIPs.put(ip, cur);
			Logger.log(ip + " closed.");
			try {
				if (work.s != null) work.s.close();
			}catch (IOException e) {
				Logger.logError(e);
			}
		}
	}
}
