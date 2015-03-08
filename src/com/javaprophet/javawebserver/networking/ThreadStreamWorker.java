package com.javaprophet.javawebserver.networking;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketException;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.http.ResponseGenerator;
import com.javaprophet.javawebserver.http.StatusCode;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.javaloader.ChunkedOutputStream;
import com.javaprophet.javawebserver.util.Logger;

public class ThreadStreamWorker extends Thread {
	private final Work work;
	private final RequestPacket req;
	private final ResponsePacket resp;
	
	public ThreadStreamWorker(Work work, RequestPacket req, ResponsePacket resp) {
		this.work = work;
		this.req = req;
		this.resp = resp;
		this.setDaemon(true);
	}
	
	public void run() {
		FileInputStream fin = null;
		// resp.headers.removeHeaders("Content-Encoding");
		try {
			fin = new FileInputStream(JavaWebServer.fileManager.getAbsolutePath(resp.body.loc, req)); // TODO: bounded ranges
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
			ChunkedOutputStream cos = new ChunkedOutputStream(work.out, resp, resp.headers.hasHeader("Content-Encoding") && resp.headers.getHeader("Content-Encoding").contains("gzip"));
			cos.writeHeaders();
			int i = 1;
			byte[] buf = new byte[10485760];
			int wr = 0;
			while (!work.s.isClosed() && i > 0) {
				i = fin.read(buf);
				wr += i;
				if (i < 1) {
					work.s.close();
					break;
				}else {
					cos.write(buf, 0, i);
					cos.flush();
				}
			}
			cos.finish();
			// cos.close();
			ThreadWorker.readdWork(work);
		}catch (IOException e) {
			if (!(e instanceof SocketException)) Logger.logError(e);
		}finally {
			String ip = work.s.getInetAddress().getHostAddress();
			Integer cur = ThreadWorker.connIPs.get(ip);
			if (cur == null) cur = 1;
			cur -= 1;
			ThreadWorker.connIPs.put(ip, cur);
			Logger.log(ip + " closed.");
			try {
				if (fin != null) fin.close();
				if (work.s != null) work.s.close();
			}catch (IOException e) {
				Logger.logError(e);
			}
		}
	}
}
