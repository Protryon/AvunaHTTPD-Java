package com.javaprophet.javawebserver.networking;

import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.http.ContentEncoding;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;

public class ThreadPipeline extends Thread {
	private final Connection c;
	public final RequestPacket request;
	public final ResponsePacket response;
	public boolean finished = false;
	public ContentEncoding use = ContentEncoding.identity;
	
	public ThreadPipeline(Connection c, RequestPacket request, ResponsePacket response) {
		this.c = c;
		this.request = request;
		this.response = response;
	}
	
	public void run() {
		System.out.println(request.toString());
		JavaWebServer.pluginBus.processPacket(request);
		Connection.rg.process(request, response);
		JavaWebServer.pluginBus.processPacket(response);
		ContentEncoding use = ContentEncoding.identity;
		if (request.headers.hasHeader("Accept-Encoding")) {
			String[] ces = request.headers.getHeader("Accept-Encoding").value.split(",");
			ContentEncoding[] ces2 = new ContentEncoding[ces.length];
			for (int i = 0; i < ces.length; i++) {
				ces2[i] = ContentEncoding.get(ces[i].trim());
			}
			
			for (ContentEncoding ce : ces2) {
				if (ce == ContentEncoding.gzip) {
					use = ce;
					break;
				}else if (ce == ContentEncoding.xgzip) {
					use = ce;
					break;
				}
			}
		}
		finished = true;
		ThreadPipeline pl;
		while ((pl = c.pipeQueue.poll()) != null && pl.finished) {
			c.finishedPipeQueue.add(pl);
			if (pl == this) break;
		}
		System.out.println(response.toString2(use));
	}
}
