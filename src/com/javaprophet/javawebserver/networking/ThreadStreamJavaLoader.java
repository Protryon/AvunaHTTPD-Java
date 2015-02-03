package com.javaprophet.javawebserver.networking;

import java.io.IOException;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.javaloader.ChunkedOutputStream;
import com.javaprophet.javawebserver.plugins.javaloader.JavaLoaderStream;
import com.javaprophet.javawebserver.util.Logger;

public class ThreadStreamJavaLoader extends Thread {
	private final Work work;
	private final RequestPacket req;
	private final ResponsePacket resp;
	private final JavaLoaderStream jls;
	
	public ThreadStreamJavaLoader(Work work, RequestPacket req, ResponsePacket resp, JavaLoaderStream jls) {
		this.work = work;
		this.req = req;
		this.resp = resp;
		this.jls = jls;
	}
	
	public void run() {
		try {
			ChunkedOutputStream cos = new ChunkedOutputStream(work.out, resp, resp.headers.hasHeader("Content-Encoding") && resp.headers.getHeader("Content-Encoding").contains("gzip"));
			jls.generate(cos, req, resp);
			cos.finish();
			ThreadWorker.readdWork(work);
		}catch (IOException e) {
			Logger.logError(e);
		}
	}
}
