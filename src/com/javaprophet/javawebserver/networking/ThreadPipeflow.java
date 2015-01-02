package com.javaprophet.javawebserver.networking;

import java.io.IOException;
import java.net.SocketException;
import java.util.Date;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;

public class ThreadPipeflow extends Thread {
	private final ConnectionJWS c;
	
	public ThreadPipeflow(ConnectionJWS c) {
		this.c = c;
	}
	
	public void run() {
		while (!c.s.isClosed()) {
			ThreadPipeline pipeline;
			try {
				pipeline = c.finishedPipeQueue.take();
			}catch (InterruptedException e1) {
				e1.printStackTrace();
				continue;
			}
			if (pipeline != null && pipeline.finished) {
				c.pipeQueue.poll();
				try {
					ResponsePacket fp = pipeline.response.write(c.out);
					System.out.println("[" + Connection.timestamp.format(new Date()) + "]" + fp.request.userIP + " requested " + pipeline.request.target + " returned " + fp.statusCode + " " + fp.reasonPhrase);
				}catch (IOException e) {
					if (!(e instanceof SocketException)) e.printStackTrace();
				}
			}
		}
	}
}
