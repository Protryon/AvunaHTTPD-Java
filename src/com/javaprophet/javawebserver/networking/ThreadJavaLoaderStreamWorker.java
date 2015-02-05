package com.javaprophet.javawebserver.networking;

import java.io.IOException;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.javaloader.ChunkedOutputStream;
import com.javaprophet.javawebserver.plugins.javaloader.JavaLoaderStream;
import com.javaprophet.javawebserver.util.Logger;

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
	}
	
	public void run() {
		try {
			ChunkedOutputStream cos = new ChunkedOutputStream(work.out, resp, resp.headers.hasHeader("Content-Encoding") && resp.headers.getHeader("Content-Encoding").contains("gzip"));
			reqStream.generate(cos, req, resp);
			ThreadWorker.readdWork(work);
		}catch (IOException e) {
			Logger.logError(e);
		}finally {
			
		}
	}
}
