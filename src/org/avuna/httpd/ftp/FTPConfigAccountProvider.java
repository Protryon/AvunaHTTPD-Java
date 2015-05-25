package org.avuna.httpd.ftp;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.Logger;

public class FTPConfigAccountProvider extends FTPAccountProvider {
	private final ConfigNode node;
	
	public FTPConfigAccountProvider(ConfigNode node) {
		this.node = node;
		if (!node.containsNode("root")) node.insertNode("root", "/");
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
	public String getRoot(String user) {
		return node.getNode("root").getValue();
	}
	
	public int getUID(String user) {
		return -1;
	}
	
	@Override
	public String getExternalIP() {
		return node.getNode("ourip").getValue();
	}
}
