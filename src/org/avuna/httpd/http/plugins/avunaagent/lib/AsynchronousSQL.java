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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import javax.sql.rowset.CachedRowSet;
import org.avuna.httpd.util.Logger;
import com.sun.rowset.CachedRowSetImpl;

public class AsynchronousSQL {
	private final DatabaseManager manager;
	private int idc = 0;
	
	private final ArrayList<AQuery> aqs = new ArrayList<AQuery>();
	private final ArrayList<AQuery> aqsm = new ArrayList<AQuery>();
	private final ThreadRunManaged trm = new ThreadRunManaged();
	
	private final class ThreadRunManaged extends Thread {
		public ThreadRunManaged() {
			super("Async Query Thread");
			this.setDaemon(true);
		}
		
		public void run() {
			try {
				while (true) {
					for (AQuery aq : aqsm) {
						if (aq.nextrun >= System.currentTimeMillis()) {
							aq.run();
						}
					}
					try {
						Thread.sleep(100L);
					}catch (InterruptedException e) {
						Logger.logError(e);
					}
				}
			}catch (SQLException e) {
				Logger.logError(e);
			}
		}
	}
	
	private final ArrayBlockingQueue<String> toExec = new ArrayBlockingQueue<String>(5000);
	
	private final class ThreadAsyncExecute extends Thread {
		public ThreadAsyncExecute() {
			super("Async Execute Thread");
			setDaemon(true);
		}
		
		public boolean running = true;
		
		public void run() {
			while (running) {
				String query = toExec.poll();
				if (query == null) {
					try {
						Thread.sleep(10L);
					}catch (InterruptedException e) {
						Logger.logError(e);
					}
					continue;
				}
				Statement stmt = null;
				try {
					stmt = manager.leaseStatement();
					stmt.execute(query);
				}catch (SQLException e) {
					Logger.logError(e);
				}finally {
					if (stmt != null) manager.returnStatement(stmt);
				}
			}
		}
	}
	
	public void destroy() {
		
	}
	
	private boolean mr = false;
	
	private static final class AQuery {
		private final int id, interval;
		private final String query;
		private long nextrun = 0;
		private final DatabaseManager manager;
		private final boolean isQuery, alwaysRun;
		private CachedRowSet crs = null;
		
		public AQuery(DatabaseManager manager, int id, String query, int interval, boolean isQuery, boolean alwaysRun) {
			this.id = id;
			this.query = query;
			this.interval = interval;
			if (interval > 0) {
				nextrun = System.currentTimeMillis() + interval;
			}else {
				nextrun = -1;
			}
			this.manager = manager;
			this.isQuery = isQuery;
			this.alwaysRun = alwaysRun;
		}
		
		public void run() throws SQLException {
			PreparedStatement leased = manager.leasePStatement(query);
			if (isQuery) {
				ResultSet rs = leased.executeQuery();
				CachedRowSet crs = new CachedRowSetImpl();
				crs.populate(rs);
				this.crs = crs;
			}else {
				leased.execute();
			}
			manager.returnPStatement(query, leased);
			if (nextrun >= 0) {
				nextrun = System.currentTimeMillis() + interval;
			}
		}
		
	}
	
	private ArrayList<ThreadAsyncExecute> thds = new ArrayList<ThreadAsyncExecute>();
	
	public AsynchronousSQL(DatabaseManager manager, int threadCount) {
		this.manager = manager;
		trm.setDaemon(true);
		for (int i = 0; i < threadCount; i++) {
			ThreadAsyncExecute tae = new ThreadAsyncExecute();
			tae.start();
			thds.add(tae);
		}
	}
	
	public AsynchronousSQL(DatabaseManager manager) {
		this(manager, 1);
	}
	
	public int addQuery(String query, int interval, boolean isQuery, boolean alwaysRun) {
		if (alwaysRun && interval < 1) {
			throw new NullPointerException("Custom run && alwaysRun cannot be enabled at the same time.");
		}
		AQuery aq = new AQuery(manager, idc++, query, interval, isQuery, alwaysRun);
		aqs.add(aq);
		if (alwaysRun) {
			aqsm.add(aq);
			if (!mr) {
				mr = true;
				trm.start();
			}
		}
		return idc - 1;
	}
	
	public void runQuery(final int id) {
		Thread thr = new Thread() {
			public void run() {
				AQuery aq = null;
				for (AQuery aql : aqs) {
					if (aql.id == id) {
						aq = aql;
						break;
					}
				}
				if (aq != null) {
					try {
						aq.run();
					}catch (SQLException e) {
						Logger.logError(e);
					}
				}
			}
		};
		thr.setDaemon(true);
		thr.start();// TODO: make worker threads
	}
	
	public void syncQuery(final int id) {
		AQuery aq = null;
		for (AQuery aql : aqs) {
			if (aql.id == id) {
				aq = aql;
				break;
			}
		}
		if (aq != null) {
			try {
				aq.run();
			}catch (SQLException e) {
				Logger.logError(e);
			}
		}
	}
	
	public void runAsync(String execute) {
		toExec.add(execute);
	}
	
	public ResultSet getQuery(int id) {
		AQuery aq = null;
		for (AQuery aql : aqs) {
			if (aql.id == id) {
				aq = aql;
				break;
			}
		}
		if (aq == null || !aq.isQuery) {
			throw new NullPointerException();
		}
		if (!aq.alwaysRun && (aq.crs == null || (aq.nextrun > 0 && aq.nextrun <= System.currentTimeMillis()))) {
			try {
				aq.run();
			}catch (SQLException e) {
				Logger.logError(e);
			}
		}
		try {
			return aq.crs.createShared();
		}catch (SQLException e) {
			Logger.logError(e);
			return null;
		}
	}
}
