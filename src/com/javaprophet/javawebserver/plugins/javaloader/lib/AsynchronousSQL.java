package com.javaprophet.javawebserver.plugins.javaloader.lib;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import javax.sql.rowset.CachedRowSet;
import com.javaprophet.javawebserver.util.Logger;
import com.sun.rowset.CachedRowSetImpl;

public class AsynchronousSQL {
	private final DatabaseManager manager;
	private int idc = 0;
	
	private final ArrayList<AQuery> aqs = new ArrayList<AQuery>();
	private final ArrayList<AQuery> aqsm = new ArrayList<AQuery>();
	private final ThreadRunManaged trm = new ThreadRunManaged();
	
	private final class ThreadRunManaged extends Thread {
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
	
	public void destroy() {
		
	}
	
	private boolean mr = false;
	
	private static final class AQuery {
		public final int id, interval;
		public final String query;
		public long nextrun = 0;
		private final DatabaseManager manager;
		public final boolean isQuery, alwaysRun;
		public CachedRowSet crs = null;
		
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
			Statement leased = manager.leaseStatement();
			if (isQuery) {
				ResultSet rs = leased.executeQuery(query);
				CachedRowSet crs = new CachedRowSetImpl();
				crs.populate(rs);
				this.crs = crs;
			}else {
				leased.execute(query);
			}
			manager.returnStatement(leased);
			if (nextrun >= 0) {
				nextrun = System.currentTimeMillis() + interval;
			}
		}
		
	}
	
	public AsynchronousSQL(DatabaseManager manager) {
		this.manager = manager;
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
	
	public void runQuery(int id) {
		AQuery aq = null;
		for (AQuery aql : aqs) {
			if (aql.id == id) {
				aq = aql;
				break;
			}
		}
		if (aq == null) {
			throw new NullPointerException();
		}
		try {
			aq.run();
		}catch (SQLException e) {
			Logger.logError(e);
		}
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
		if (!aq.alwaysRun && aq.nextrun >= System.currentTimeMillis()) {
			try {
				aq.run();
			}catch (SQLException e) {
				Logger.logError(e);
			}
		}
		return aq.crs;
	}
}
