package com.javaprophet.javawebserver.networking;

import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;

public class ThreadPipeline extends Thread {
	private final ConnectionJWS c;
	public final RequestPacket request;
	public final ResponsePacket response;
	public boolean finished = false;
	
	public ThreadPipeline(ConnectionJWS c, RequestPacket request, ResponsePacket response) {
		this.c = c;
		this.request = request;
		this.response = response;
	}
	
	public void run() {
		System.out.println(request.toString());
		JavaWebServer.patchBus.processPacket(request);
		JavaWebServer.rg.process(request, response);
		JavaWebServer.patchBus.processPacket(response);
		finished = true;
		ThreadPipeline pl;
		while ((pl = c.pipeQueue.poll()) != null && pl.finished) {
			c.finishedPipeQueue.add(pl);
			if (pl == this) break;
		}
	}
}
