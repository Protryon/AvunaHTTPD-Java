package com.javaprophet.javawebserver.plugins.javaloader;

import java.sql.SQLException;

public class JavaLoaderUtil {
	public static String mysql_real_escape_string(java.sql.Connection link, String str) throws SQLException {
		if (str == null) {
			return "NULL";
		}
		
		if (str.replaceAll("[a-zA-Z0-9_!@#$%^&*()-=+~.;:,\\Q[\\E\\Q]\\E<>{}\\/? ]", "").length() < 1) {
			return str;
		}
		
		String clean_string = str;
		clean_string = clean_string.replaceAll("\\\\", "\\\\\\\\");
		clean_string = clean_string.replaceAll("\\n", "\\\\n");
		clean_string = clean_string.replaceAll("\\r", "\\\\r");
		clean_string = clean_string.replaceAll("\\t", "\\\\t");
		clean_string = clean_string.replaceAll("\\00", "\\\\0");
		clean_string = clean_string.replaceAll("'", "\\\\'");
		clean_string = clean_string.replaceAll("\\\"", "\\\\\"");
		
		if (clean_string.replaceAll("[a-zA-Z0-9_!@#$%^&*()-=+~.;:,\\Q[\\E\\Q]\\E<>{}\\/?\\\\\"' ]", "").length() < 1) {
			return clean_string;
		}
		
		java.sql.Statement stmt = link.createStatement();
		String qry = "SELECT QUOTE('" + clean_string + "')";
		
		stmt.executeQuery(qry);
		java.sql.ResultSet resultSet = stmt.getResultSet();
		resultSet.first();
		String r = resultSet.getString(1);
		return r.substring(1, r.length() - 1);
	}
}
