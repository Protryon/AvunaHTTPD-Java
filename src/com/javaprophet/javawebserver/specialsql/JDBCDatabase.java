package com.javaprophet.javawebserver.specialsql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author lucamasira
 * Default implementation of the IDatabase interface.<br>
 * <br>
 * There is no option to change SQL drivers since there should be an implementation for each type of driver.<br>
 * To make this simpler you could extend to the default implementation and override the connect method.
 */
public class JDBCDatabase implements IDatabase {

	/**
	 * Connection to the database
	 */
	private Connection connection;
	
	/**
	 * The timeout in seconds for statements.
	 */
	private int queryTimeout = 30;//default timeout
	
	/**
	 * Constructor which doesn't do anything.
	 */
	public JDBCDatabase(){
	}
	
	@Override
	/**
	 * Connect to a database with the corresponding connection string.
	 * @param connection the connection thing like jdbc:sql:data.db
	 * @throws SQLException an exception that can occur when connecting to the database
	 */
	public void connect(final String connection) throws SQLException{
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			/*
			 * Had to do a try/catch block since the interface only allows us to throw an SQLException.<br>
			 * Other imnplementations may not throw such exceptions so we put ours in a try/catch block.
			 */
			e.printStackTrace();
		}
		this.connection = DriverManager.getConnection(connection);
	}

	@Override
	/**
	 * Disconnect from the database
	 * @throws SQLException an exception that may occur while disconnecting from a database.
	 */
	public void disconnect() throws SQLException {
		getConnection().close();
	}

	@Override
	/**
	 * Get the database connection.
	 * @return the database connection
	 */
	public Connection getConnection() {	
		return connection;
	}
	
	@Override
	/**
	 * Create a new statement.
	 * @return a new statement
	 */
	public Statement getNewStatement() throws SQLException {
		Statement statement = getConnection().createStatement();
		statement.setQueryTimeout(queryTimeout);
		return statement;
	}

	@Override
	/**
	 * Execute a SQL query and get its result
	 * @param query the query to execute
	 * @return the retunred informated
	 * @throws SQLException an exception that may occur while executing the query
	 */
	public ResultSet execQuery(final String query) throws SQLException {
		return execQuery(getNewStatement(), query);
	}

	@Override
	/**
	 * Execute a query form a specific statement.
	 * @param statement the statement to execute the query from
	 * @param query the query to execute 
	 * @return the result of the query after being executed
	 * @throws SQLException an exception that may occur while executing the query
	 */
	public ResultSet execQuery(final Statement statement, final String query) throws SQLException {	
		return statement.executeQuery(query);
	}
	
	@Override
	/**
	 * Execute a query which doesn't return anything, such as UPDATE
	 * @param query the query to execute
	 * @throws SQLException an exception that can occcur when executing the query
	 */
	public void execUpdateQuery(final String query) throws SQLException {
		execUpdateQuery(getNewStatement(), query);
	}
	
	@Override
	/**
	 * Execute a query which doesn't return anything, such as UPDATE
	 * @param statement the statement to execute the query from
	 * @param query the query to execute
	 * @throws SQLException an exception that can occcur when executing the query
	 */
	public void execUpdateQuery(final Statement statement, final String query) throws SQLException {
		statement.executeUpdate(query);
	}


	@Override
	/**
	 * Get the size of a result(the rows)
	 * @param result the result to get the size from
	 * @return the size of the rows
	 * @throws SQLException an exception that may occur while getting the row size
	 */
	public int getResultSize(final ResultSet result) throws SQLException {
		int counter = 0;
		while(result.next())
			counter++;
		
		return counter;
	}

	@Override
	/**
	 * Set the timeout for statements.
	 * @param seconds the timeout in seconds.
	 */
	public void setQueryTimeout(final int timeout) {
		queryTimeout = timeout;
	}

	@Override
	/**
	 * Get the query timeout in seconds.
	 * @return the query timeout in seconds
	 */
	public int getQueryTimeout() {
		return queryTimeout;
	}

}
