/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins.avunaagent.lib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import org.avuna.httpd.hosts.VHost;

public class Scheduler {
	private final ArrayBlockingQueue<Runnable> workQueue;
	private boolean working = true;
	private final VHost vhost;
	
	private final class ThreadWorker extends Thread {
		public void run() {
			while (working) {
				Runnable work = workQueue.poll();
				if (work == null) {
					try {
						Thread.sleep(5L);
					}catch (InterruptedException e) {
						vhost.logger.logError(e);
					}
					continue;
				}
				try {
					work.run();
				}catch (Exception e) {
					vhost.logger.logError(e);
				}
			}
		}
	}
	
	public Scheduler(VHost vhost, int workerCount, int queueSize) {
		this.vhost = vhost;
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
	
	private final class PassiveRunnable implements Runnable {
		public PassiveRunnable(Runnable run, int msPerRun) {
			this.run = run;
			this.msPerRun = msPerRun;
		}
		
		public Runnable run;
		public long msPerRun;
		public long lastRun;
		
		public void run() {
			run.run();
		}
	}
	
	private final List<PassiveRunnable> apr = Collections.synchronizedList(new ArrayList<PassiveRunnable>());
	
	public int addPassive(Runnable run, int msPerRun) {
		synchronized (apr) {
			PassiveRunnable pr = new PassiveRunnable(run, msPerRun);
			apr.add(pr);
			pr.lastRun = System.currentTimeMillis();
			run.run();
			return apr.size() - 1;
		}
	}
	
	public Runnable runPassive(int id) {
		if (id < 0 || id >= apr.size()) throw new IllegalArgumentException("Invalid ID!");
		PassiveRunnable r = apr.get(id);
		if (r.lastRun + r.msPerRun < System.currentTimeMillis()) {
			r.lastRun = System.currentTimeMillis();
			r.run.run();
		}
		return r.run;
	}
}
