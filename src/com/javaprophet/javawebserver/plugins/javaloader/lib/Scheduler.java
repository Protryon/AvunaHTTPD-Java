package com.javaprophet.javawebserver.plugins.javaloader.lib;

import java.util.concurrent.ArrayBlockingQueue;
import com.javaprophet.javawebserver.util.Logger;

public class Scheduler {
	private final ArrayBlockingQueue<Runnable> workQueue;
	private boolean working = true;
	
	private final class ThreadWorker extends Thread {
		public void run() {
			while (working) {
				Runnable work = workQueue.poll();
				if (work == null) {
					try {
						Thread.sleep(1L);
					}catch (InterruptedException e) {
						Logger.logError(e);
					}
					continue;
				}
				try {
					work.run();
				}catch (Exception e) {
					Logger.logError(e);
				}
			}
		}
	}
	
	public Scheduler(int workerCount, int queueSize) {
		if (workerCount <= 0) throw new IllegalArgumentException("WorkerCount must be >= 1");
		if (queueSize <= 0) throw new IllegalArgumentException("QueueSize must be >= 1");
		workQueue = new ArrayBlockingQueue<Runnable>(queueSize);
		for (int i = 0; i < workerCount; i++) {
			ThreadWorker worker = new ThreadWorker();
			worker.setDaemon(true);
			worker.start();
		}
	}
	
	public void terminate() {
		working = false;
	}
	
	public void runASAP(Runnable run) {
		workQueue.add(run);
	}
	
}
