package com.javaprophet.javawebserver.networking;

import java.io.FileInputStream;
import java.io.IOException;
import com.javaprophet.javawebserver.JavaWebServer;
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
	}
	
	public void run() {
		FileInputStream fin = null;
		try {
			ChunkedOutputStream cos = new ChunkedOutputStream(work.out, resp, resp.headers.hasHeader("Content-Encoding") && resp.headers.getHeader("Content-Encoding").contains("gzip"));
			cos.writeHeaders();
			fin = new FileInputStream(JavaWebServer.fileManager.getAbsolutePath(resp.body.getBody().loc));
			int i = 1;
			byte[] buf = new byte[10485760];
			while (!work.s.isClosed() && i > 0) {
				i = fin.read(buf);
				if (i < 1) {
					work.s.close();
					break;
				}else {
					cos.write(buf, 0, i);
					cos.flush();
				}
			}
			cos.finish();
			ThreadWorker.readdWork(work);
		}catch (IOException e) {
			Logger.logError(e);
		}finally {
			try {
				if (fin != null) fin.close();
			}catch (IOException e) {
				Logger.logError(e);
			}
		}
	}
}
