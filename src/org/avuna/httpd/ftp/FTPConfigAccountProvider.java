/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.ftp;

import java.io.File;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostFTP;
import org.avuna.httpd.util.CLib;
import org.avuna.httpd.util.ConfigNode;

public class FTPConfigAccountProvider extends FTPAccountProvider {
	private final ConfigNode node;
	
	public FTPConfigAccountProvider(HostFTP host, ConfigNode node) {
		this.node = node;
		if (!node.containsNode("ourip")) try {
			node.insertNode("ourip", Inet4Address.getLocalHost().getHostAddress(), "should be set to your public ip");
		}catch (UnknownHostException e) {
			host.logger.logError(e);
			node.insertNode("ourip", "127.0.0.1", "should be set to your public ip");
		}
		if (!node.containsNode("users")) node.insertNode("users", null);
		ConfigNode users = node.getNode("users");
		for (String nn : users.getSubnodes()) {
			ConfigNode user = users.getNode(nn);
			if (!user.containsNode("pass")) user.insertNode("pass", "password");
			if (!user.containsNode("root")) user.insertNode("root", AvunaHTTPD.windows ? "C:\\" : "/");
			if (!AvunaHTTPD.windows && !CLib.failed) {
				if (!user.containsNode("uid")) user.insertNode("uid", CLib.getuid() + "", "simulated uid");
				if (!user.containsNode("gid")) user.insertNode("gid", CLib.getgid() + "", "simulated gid");
			}
		}
	}
	
	@Override
	public boolean isValid(String user, String pass) {
		ConfigNode users = node.getNode("users");
		if (!users.containsNode(user)) return false;
		ConfigNode usn = users.getNode(user);
		if (!usn.branching()) return false;
		return usn.containsNode("pass") && usn.getValue("pass").equals(pass);
	}
	
	@Override
	public File getRoot(String user) {
		ConfigNode users = node.getNode("users");
		if (!users.containsNode(user)) return null;
		ConfigNode usn = users.getNode(user);
		if (!usn.containsNode("root")) return null;
		return new File(usn.getNode("root").getValue());
	}
	
	public int getUID(String user) {
		if (AvunaHTTPD.windows || CLib.failed) return -1;
		ConfigNode users = node.getNode("users");
		if (!users.containsNode(user)) return -1;
		ConfigNode usn = users.getNode(user);
		if (!usn.containsNode("root")) return -1;
		return Integer.parseInt(usn.getNode("uid").getValue());
	}
	
	public int getGID(String user) {
		if (AvunaHTTPD.windows || CLib.failed) return -1;
		ConfigNode users = node.getNode("users");
		if (!users.containsNode(user)) return -1;
		ConfigNode usn = users.getNode(user);
		if (!usn.containsNode("root")) return -1;
		return Integer.parseInt(usn.getNode("gid").getValue());
	}
	
	@Override
	public String getExternalIP() {
		return node.getNode("ourip").getValue();
	}
}
