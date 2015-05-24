package org.avuna.httpd.ftp;

import org.avuna.httpd.util.ConfigNode;

public class FTPConfigAccountProvider extends FTPAccountProvider {
	private final ConfigNode node;
	
	public FTPConfigAccountProvider(ConfigNode node) {
		this.node = node;
		if (!node.containsNode("root")) node.insertNode("root", "/");
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
}
