/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins.avunaagent.lib;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.avuna.httpd.hosts.VHost;
import org.avuna.httpd.http.plugins.avunaagent.AvunaAgent;
import org.avuna.httpd.http.plugins.avunaagent.AvunaAgentSecurity;
import org.avuna.httpd.http.plugins.avunaagent.PluginAvunaAgent;

public class AvunaAgentUtil {
	public static String htmlescape(String html) {
		return html.replace("&", "&amp;").replace("\"", "&quot;").replace("'", "&#039;").replace("<", "&lt;").replace(">", "&gt;");
	}
	
	public static String mysql_real_escape_string(DatabaseManager manager, String str) throws SQLException {
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
		Statement stmt = manager.leaseStatement();
		stmt.executeQuery("SELECT QUOTE('" + clean_string + "')");
		ResultSet resultSet = stmt.getResultSet();
		resultSet.first();
		String r = resultSet.getString(1);
		manager.returnStatement(stmt);
		return r.substring(1, r.length() - 1);
	}
	
	public static AvunaAgent getJavaLoaderByClass(VHost host, Class<? extends AvunaAgent> jlc) {
		return getJavaLoaderByClass(host, jlc.getName());
	}
	
	public static AvunaAgent getJavaLoaderByClass(VHost host, String jlc) {
		if (host != null) {
			if (host.getJLS().getJLS().containsKey(jlc)) {
				return host.getJLS().getJLS().get(jlc);
			}
		}else {
			for (AvunaAgentSecurity jls : PluginAvunaAgent.security) {
				if (jls.getClass().getName().equals(jlc)) {
					return jls;
				}
			}
		}
		return null;
	}
}
