package com.javaprophet.javawebserver.networking;

import java.io.IOException;
import java.net.SocketException;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.util.Logger;

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
					Logger.INSTANCE.log(fp.request.userIP + " requested " + pipeline.request.target + " returned " + fp.statusCode + " " + fp.reasonPhrase);
				}catch (IOException e) {
					if (!(e instanceof SocketException)) e.printStackTrace();
				}
			}
		}
	}
}
