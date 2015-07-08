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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

public class DatabaseManager {
	private Connection conn;
	private ArrayList<ExtendedStatement> estmts = new ArrayList<ExtendedStatement>();
	private static ArrayList<DatabaseManager> open = new ArrayList<DatabaseManager>();
	private HashMap<String, ArrayList<ExtendedPStatement>> pstmts = new HashMap<String, ArrayList<ExtendedPStatement>>();
	private final String jdbc;
	
	public static void closeAll() throws SQLException {
		for (DatabaseManager db : open) {
			db.close();
		}
	}
	
	public DatabaseManager(String driver, String ip, String db, String user, String pass) throws SQLException {
		conn = DriverManager.getConnection(jdbc = "jdbc:" + driver + "://" + ip + "/" + db + "?user=" + user + "&password=" + pass);
	}
	
	private static class ExtendedStatement {
		public final Statement stmt;
		public boolean leased = false;
		
		public ExtendedStatement(Statement stmt) {
			this.stmt = stmt;
		}
	}
	
	private static class ExtendedPStatement {
		public final PreparedStatement stmt;
		public boolean leased = false;
		
		public ExtendedPStatement(PreparedStatement stmt) {
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
	
	public void reconnect() throws SQLException {
		synchronized (conn) {
			conn = DriverManager.getConnection(jdbc);
		}
		synchronized (estmts) {
			estmts.clear();
		}
		synchronized (pstmts) {
			for (String key : pstmts.keySet()) {
				pstmts.get(key).clear();
			}
		}
	}
	
	public Statement leaseStatement() throws SQLException {
		if (conn.isClosed()) {
			reconnect();
		}
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
	
	public PreparedStatement leasePStatement(String sql) throws SQLException {
		if (conn.isClosed()) {
			reconnect();
		}
		synchronized (pstmts) {
			if (!pstmts.containsKey(sql)) pstmts.put(sql, new ArrayList<ExtendedPStatement>());
			for (ExtendedPStatement pstmt : pstmts.get(sql)) {
				if (!pstmt.leased) {
					pstmt.leased = true;
					return pstmt.stmt;
				}
			}
		}
		ExtendedPStatement pstmt = new ExtendedPStatement(conn.prepareStatement(sql));
		pstmt.leased = true;
		synchronized (estmts) {
			pstmts.get(sql).add(pstmt);
		}
		return pstmt.stmt;
	}
	
	public void returnPStatement(String sql, PreparedStatement stmt) {
		synchronized (pstmts) {
			for (ExtendedPStatement pstmt : pstmts.get(sql)) {
				if (pstmt.stmt.equals(stmt)) {
					pstmt.leased = false;
				}
			}
		}
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
