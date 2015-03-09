package com.javaprophet.javawebserver.plugins.javaloader.lib;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import com.javaprophet.javawebserver.hosts.VHost;
import com.javaprophet.javawebserver.plugins.javaloader.JavaLoader;
import com.javaprophet.javawebserver.plugins.javaloader.JavaLoaderSecurity;
import com.javaprophet.javawebserver.plugins.javaloader.PatchJavaLoader;

public class JavaLoaderUtil {
	public static String mysql_real_escape_string(Statement stmt, String str) throws SQLException {
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
		stmt.executeQuery("SELECT QUOTE('" + clean_string + "')");
		ResultSet resultSet = stmt.getResultSet();
		resultSet.first();
		String r = resultSet.getString(1);
		return r.substring(1, r.length() - 1);
	}
	
	public static JavaLoader getJavaLoaderByClass(VHost host, Class<? extends JavaLoader> jlc) {
		return getJavaLoaderByClass(host, jlc.getName());
	}
	
	public static JavaLoader getJavaLoaderByClass(VHost host, String jlc) {
		if (host != null) {
			if (host.getJLS().getJLS().containsKey(jlc)) {
				return host.getJLS().getJLS().get(jlc);
			}
		}else {
			for (JavaLoaderSecurity jls : PatchJavaLoader.security) {
				if (jls.getClass().getName().equals(jlc)) {
					return jls;
				}
			}
		}
		return null;
	}
}
