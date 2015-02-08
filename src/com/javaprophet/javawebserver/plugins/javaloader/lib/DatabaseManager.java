package com.javaprophet.javawebserver.plugins.javaloader.lib;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class DatabaseManager {
	private Connection conn;
	private ArrayList<ExtendedStatement> estmts = new ArrayList<ExtendedStatement>();
	private static ArrayList<DatabaseManager> open = new ArrayList<DatabaseManager>();
	
	public static void closeAll() throws SQLException {
		for (DatabaseManager db : open) {
			db.close();
		}
	}
	
	public DatabaseManager(String driver, String ip, String db, String user, String pass) throws SQLException {
		conn = DriverManager.getConnection("jdbc:" + driver + "://" + ip + "/" + db + "?user=" + user + "&password=" + pass);
	}
	
	private static class ExtendedStatement {
		public final Statement stmt;
		public boolean leased = false;
		
		public ExtendedStatement(Statement stmt) {
			this.stmt = stmt;
		}
	}
	
	public Statement getIndependentStatement() throws SQLException {
		return conn.createStatement();
	}
	
	public void close() throws SQLException {
		synchronized (estmts) {
			for (ExtendedStatement estmt : estmts) {
				estmt.stmt.close();
			}
		}
		conn.close();
	}
	
	public Statement leaseStatement() throws SQLException {
		synchronized (estmts) {
			for (ExtendedStatement estmt : estmts) {
				if (!estmt.leased) {
					estmt.leased = true;
					return estmt.stmt;
				}
			}
		}
		ExtendedStatement estmt = new ExtendedStatement(conn.createStatement());
		estmt.leased = true;
		synchronized (estmts) {
			estmts.add(estmt);
		}
		return estmt.stmt;
	}
	
	public void returnStatement(Statement stmt) {
		synchronized (estmts) {
			for (ExtendedStatement estmt : estmts) {
				if (estmt.stmt.equals(stmt)) {
					estmt.leased = false;
				}
			}
		}
	}
	
}
