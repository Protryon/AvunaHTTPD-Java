/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.ftp;

import java.io.File;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.Logger;

public class FTPConfigAccountProvider extends FTPAccountProvider {
	private final ConfigNode node;
	private final int uid, gid;
	
	public FTPConfigAccountProvider(ConfigNode node) {
		this.node = node;
		if (!node.containsNode("root")) node.insertNode("root", "/");
		if (!AvunaHTTPD.windows) {
			if (!node.containsNode("uid")) node.insertNode("uid", AvunaHTTPD.mainConfig.getNode("uid").getValue());
			if (!node.containsNode("gid")) node.insertNode("gid", AvunaHTTPD.mainConfig.getNode("gid").getValue());
			this.uid = Integer.parseInt(node.getNode("uid").getValue());
			this.gid = Integer.parseInt(node.getNode("gid").getValue());
		}else {
			this.uid = 0;
			this.gid = 0;
		}
		if (!node.containsNode("ourip")) try {
			node.insertNode("ourip", Inet4Address.getLocalHost().getHostAddress(), "should be set to your public ip");
		}catch (UnknownHostException e) {
			Logger.logError(e);
			node.insertNode("ourip", "127.0.0.1", "should be set to your public ip");
		}
		if (!node.containsNode("users")) node.insertNode("users", null, "format is user=pass");
	}
	
	@Override
	public boolean isValid(String user, String pass) {
		ConfigNode users = node.getNode("users");
		for (String un : users.getSubnodes()) {
			if (un.equals(user)) {
				return users.getNode(un).getValue().equals(pass);
			}
		}
		return false;
	}
	
	@Override
	public File getRoot(String user) {
		return new File(node.getNode("root").getValue());
	}
	
	public int getUID(String user) {
		return uid;
	}
	
	public int getGID(String user) {
		return gid;
	}
	
	@Override
	public String getExternalIP() {
		return node.getNode("ourip").getValue();
	}
}
