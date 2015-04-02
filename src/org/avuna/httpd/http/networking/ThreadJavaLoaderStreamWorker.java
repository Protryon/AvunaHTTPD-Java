package org.avuna.httpd.http.networking;

import java.io.IOException;
import java.net.SocketException;
import org.avuna.httpd.http.plugins.javaloader.JavaLoaderStream;
import org.avuna.httpd.util.Logger;

public class ThreadJavaLoaderStreamWorker extends Thread {
	private final Work work;
	private final RequestPacket req;
	private final ResponsePacket resp;
	private final JavaLoaderStream reqStream;
	
	public ThreadJavaLoaderStreamWorker(Work work, RequestPacket req, ResponsePacket resp, JavaLoaderStream reqStream) {
		this.work = work;
		this.req = req;
		this.resp = resp;
		this.reqStream = reqStream;
		this.setDaemon(true);
	}
	
	public void run() {
		try {
			ChunkedOutputStream cos = new ChunkedOutputStream(work.out, resp, resp.headers.hasHeader("Content-Encoding") && resp.headers.getHeader("Content-Encoding").contains("gzip"));
			reqStream.generate(cos, req, resp);
			ThreadConnection.readdWork(work);
		}catch (IOException e) {
			if (!(e instanceof SocketException)) Logger.logError(e);
		}finally {
			String ip = work.s.getInetAddress().getHostAddress();
			Integer cur = ThreadConnection.connIPs.get(ip);
			if (cur == null) cur = 1;
			cur -= 1;
			ThreadConnection.connIPs.put(ip, cur);
			Logger.log(ip + " closed.");
			try {
				if (work.s != null) work.s.close();
			}catch (IOException e) {
				Logger.logError(e);
			}
		}
	}
}
