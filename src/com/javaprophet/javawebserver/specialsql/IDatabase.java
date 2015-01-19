package com.javaprophet.javawebserver.specialsql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author lucamasira
 * Interface for plugin databases, this allows more customizability<br>
 * The default implementation of this interface already has customization but yeh.
 */
public interface IDatabase {
	
	/**
	 * Connect to a database
	 * @param sqlString the string used for the connection such as jdbc:sqlite:test.db
	 * @throws SQLException an exception that can occur when connecting to a database.
	 */
	public void connect(String sqlString) throws SQLException;
	
	/**
	 * Disconnect from the database
	 * @throws SQLException an exception that may occur while disconnecting from a database.
	 */
	public void disconnect() throws SQLException;
	
	/**
	 * Get the database connection.
	 * @return the database connection
	 */
	public Connection getConnection();
	
	/**
	 * Create a new statement.
	 * @return a new statement
	 */
	public Statement getNewStatement() throws SQLException;
	
	/**
	 * Execute a SQL query and get its result
	 * @param query the query to execute
	 * @return the retunred informated
	 * @throws SQLException an exception that may occur while executing the query
	 */
	public ResultSet execQuery(final String query) throws SQLException;
	
	/**
	 * Execute a query form a specific statement.
	 * @param statement the statement to execute the query from
	 * @param query the query to execute 
	 * @return the result of the query after being executed
	 * @throws SQLException an exception that may occur while executing the query
	 */
	public ResultSet execQuery(final Statement statement, final String query) throws SQLException;
	
	/**
	 * Execute a query which doesn't return anything, such as UPDATE
	 * @param query the query to execute
	 * @throws SQLException an exception that can occcur when executing the query
	 */
	public void execUpdateQuery(final String query) throws SQLException;
	
	/**
	 * Execute a query which doesn't return anything, such as UPDATE
	 * @param statement the statement to execute the query from
	 * @param query the query to execute
	 * @throws SQLException an exception that can occcur when executing the query
	 */
	public void execUpdateQuery(final Statement statement, final String query) throws SQLException;
	
	/**
	 * Get the size of a result(the rows)
	 * @param result the result to get the size from
	 * @return the size of the rows
	 * @throws SQLException an exception that may occur while getting the row size
	 */
	public int getResultSize(final ResultSet result) throws SQLException;
	
	/**
	 * Set the timeout for statements.
	 * @param seconds the timeout in seconds.
	 */
	public void setQueryTimeout(final int seconds);
	
	/**
	 * Get the query timeout in seconds.
	 * @return the query timeout in seconds
	 */
	public int getQueryTimeout();
}
