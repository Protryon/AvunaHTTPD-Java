package com.javaprophet.javawebserver.networking;

import java.io.IOException;

public class ThreadPipeflow extends Thread {
	private final Connection c;
	
	public ThreadPipeflow(Connection c) {
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
					pipeline.response.write(c.out);
					System.out.println(pipeline.response.toString2());
				}catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
