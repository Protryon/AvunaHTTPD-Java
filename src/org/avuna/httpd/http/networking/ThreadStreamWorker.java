/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.networking;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketException;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.http.ResponseGenerator;
import org.avuna.httpd.http.StatusCode;

public class ThreadStreamWorker extends Thread {
	private final Work work;
	private final RequestPacket req;
	private final ResponsePacket resp;
	private final HostHTTP host;
	
	public ThreadStreamWorker(HostHTTP host, Work work, RequestPacket req, ResponsePacket resp) {
		this.work = work;
		this.req = req;
		this.resp = resp;
		this.host = host;
		this.setDaemon(true);
	}
	
	public void run() {
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(AvunaHTTPD.fileManager.getAbsolutePath(resp.body.loc, req)); // TODO: bounded ranges
			resp.headers.addHeader("Accept-Ranges", "bytes");
			if (req.headers.hasHeader("Range")) {
				String range = req.headers.getHeader("Range");
				if (range.startsWith("bytes=")) {
					int ts = Integer.parseInt(range.endsWith("-") ? range.substring(6, range.length() - 1) : range.substring(6, range.indexOf("-")));
					if (ts > 0) {
						ResponseGenerator.generateDefaultResponse(resp, StatusCode.PARTIAL_CONTENT);
						resp.headers.addHeader("Content-Range", "bytes " + ts + "-" + fin.available() + "/" + (fin.available() + 1));
						fin.skip(ts);
					}
				}
			}
			@SuppressWarnings("resource")
			ChunkedOutputStream cos = new ChunkedOutputStream(work.out, resp, resp.headers.hasHeader("Content-Encoding") && resp.headers.getHeader("Content-Encoding").contains("gzip"));
			cos.writeHeaders();
			System.out.println("headers wrote");
			int i = 1;
			byte[] buf = new byte[10485760];
			while (!work.s.isClosed() && i > 0) {
				i = fin.read(buf);
				if (i < 1) {
					System.out.println("closed");
					break;
				}else {
					System.out.println("writing " + i);
					cos.write(buf, 0, i);
					System.out.println("wrote " + i);
					cos.flush();
					System.out.println("flushed");
				}
			}
			System.out.println("finished");
			cos.finish();
			// cos.close();
			host.readdWork(work);
		}catch (IOException e) {
			System.out.println("exception");
			if (!(e instanceof SocketException)) req.host.logger.logError(e);
		}finally {
			String ip = work.s.getInetAddress().getHostAddress();
			Integer cur = HostHTTP.connIPs.get(ip);
			if (cur == null) cur = 1;
			cur -= 1;
			HostHTTP.connIPs.put(ip, cur);
			req.host.logger.log(ip + " closed.");
			try {
				if (fin != null) fin.close();
				if (work.s != null) work.s.close();
			}catch (IOException e) {
				req.host.logger.logError(e);
			}
		}
	}
}
