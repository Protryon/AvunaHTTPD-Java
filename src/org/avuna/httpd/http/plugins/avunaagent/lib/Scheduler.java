/*	Avuna HTTPD - General Server Applications
    Copyright (C) 2015 Maxwell Bruce

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package org.avuna.httpd.http.plugins.avunaagent.lib;

import java.util.concurrent.ArrayBlockingQueue;
import org.avuna.httpd.util.Logger;

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
