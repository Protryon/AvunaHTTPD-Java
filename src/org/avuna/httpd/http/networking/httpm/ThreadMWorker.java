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

package org.avuna.httpd.http.networking.httpm;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.hosts.VHostM;
import org.avuna.httpd.http.Resource;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.networking.ThreadWorker;
import org.avuna.httpd.util.Logger;
import org.avuna.httpd.util.Stream;
import org.avuna.httpd.util.unixsocket.UnixSocket;

public class ThreadMWorker extends ThreadWorker {
	
	public ThreadMWorker(HostHTTP host) {
		super(host);
		this.setName("Avuna HTTPM-Worker Thread #" + (nid - 1));
	}
	
	public void run() {
		while (keepRunning) {
			RequestPacket incomingRequest = host.pollReqQueue();
			if (incomingRequest == null) {
				try {
					Thread.sleep(2L, 500000);
				}catch (InterruptedException e) {
					// Logger.logError(e);
				}
				continue;
			}
			try {
				if (!(incomingRequest.host instanceof VHostM)) {
					continue;
				}
				VHostM vh = (VHostM)incomingRequest.host;
				long benchStart = System.nanoTime();
				if (incomingRequest.work.cn == null) {
					incomingRequest.work.cn = vh.unix ? new MasterConn(new UnixSocket(vh.ip)) : new MasterConn(new Socket(vh.ip, vh.port));
				}
				try {
					incomingRequest.headers.addHeader("X-Forwarded-For", incomingRequest.work.s.getInetAddress().getHostAddress());
					incomingRequest.write(incomingRequest.work.cn.getOutputStream());
					incomingRequest.work.cn.getOutputStream().flush();
					ResponsePacket outgoingResponse = incomingRequest.child;
					outgoingResponse.request = incomingRequest;
					String line = Stream.readLine(incomingRequest.work.cn.getInputStream());
					if (line == null) {
						incomingRequest.work.cn.getSocket().close();
						incomingRequest.work.cn = vh.unix ? new MasterConn(new UnixSocket(vh.ip)) : new MasterConn(new Socket(vh.ip, vh.port));
						incomingRequest.write(incomingRequest.work.cn.getOutputStream());
						incomingRequest.work.cn.getOutputStream().flush();
						line = Stream.readLine(incomingRequest.work.cn.getInputStream());
						if (line == null) {
							Logger.log("Reconnect failed, check VHost connection!");
						}
					}
					if (line.length() == 0) {
						line = Stream.readLine(incomingRequest.work.cn.getInputStream());
					}
					int i = line.indexOf(" ");
					outgoingResponse.httpVersion = line.substring(0, i);
					i++;
					outgoingResponse.statusCode = Integer.parseInt(line.substring(i, (i = line.indexOf(" ", i))));
					i++;
					outgoingResponse.reasonPhrase = line.substring(i);
					while ((line = Stream.readLine(incomingRequest.work.cn.getInputStream())).length() > 0) {
						outgoingResponse.headers.addHeader(line);
					}
					
					if (outgoingResponse.headers.hasHeader("Content-Length")) {
						byte[] data = new byte[Integer.parseInt(outgoingResponse.headers.getHeader("Content-Length"))];
						incomingRequest.work.cn.getInputStream().readFully(data);
						// readLine(incomingRequest.work.cn.in);
						outgoingResponse.body = new Resource(data, outgoingResponse.headers.hasHeader("Content-Type") ? outgoingResponse.headers.getHeader("Content-Type") : "text/html; charset=utf-8");
						outgoingResponse.prewrite();
						outgoingResponse.done = true;
						outgoingResponse.bwt = System.nanoTime();
					}else if (outgoingResponse.headers.hasHeader("Transfer-Encoding") && outgoingResponse.headers.getHeader("Transfer-Encoding").contains("chunked")) {
						outgoingResponse.toStream = incomingRequest.work.cn.getInputStream();
						outgoingResponse.done = true;
						outgoingResponse.bwt = System.nanoTime();
					}else {
						outgoingResponse.bwt = System.nanoTime();
						// no body
					}
				}catch (Exception e) {
					Logger.logError(e);
				}finally {
				}
				// Logger.log((est - benchStart) / 1000000D + " start-est");
				// Logger.log((writ - est) / 1000000D + " est-writ");
				// Logger.log((hdr - writ) / 1000000D + " writ-hdr");
				// Logger.log((incomingRequest.child.bwt - hdr) / 1000000D + " hdr-bwt");
				// Logger.log((cls - incomingRequest.child.bwt) / 1000000D + " bwt-cls");
				if (incomingRequest.host.getDebug()) {
					Logger.log(AvunaHTTPD.crlf + incomingRequest.toString().trim());
				}
				Logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + " " + incomingRequest.host.getHostPath() + incomingRequest.target + " returned " + incomingRequest.child.statusCode + " " + incomingRequest.child.reasonPhrase + " took: " + (incomingRequest.child.bwt - benchStart) / 1000000D + " ms");
			}catch (Exception e) {
				if (!(e instanceof SocketException || e instanceof StringIndexOutOfBoundsException)) {
					Logger.logError(e);
					
				}else {
					try {
						if (incomingRequest.work.cn != null) incomingRequest.work.cn.getSocket().close();
						incomingRequest.work.s.close();
					}catch (IOException ex) {
						Logger.logError(ex);
					}
				}
			}finally {
				if (host.sizeReqQueue() < 10000) { // idle fix
					try {
						Thread.sleep(0L, 100000);
					}catch (InterruptedException e) {
						Logger.logError(e);
					}
				}
			}
		}
	}
}
