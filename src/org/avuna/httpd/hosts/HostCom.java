package org.avuna.httpd.hosts;

import java.net.ServerSocket;
import org.avuna.httpd.com.ComServer;
import org.avuna.httpd.util.ConfigNode;

public class HostCom extends Host {
	
	public HostCom(String name) {
		super(name, Protocol.COM);
	}
	
	public static void unpack() {
		
	}
	
	private String[] auth = null;
	
	public void formatConfig(ConfigNode map) {
		if (!map.containsNode("port")) map.insertNode("port", "6049", "bind port");
		if (!map.containsNode("ip")) map.insertNode("ip", "127.0.0.1", "bind ip, or unix socket file");
		if (!map.containsNode("unix")) map.insertNode("unix", "false", "set to true, and set ip to the socket file to use a unix socket. port is ignored. mostly used for shared hosting.");
		// super.formatConfig(map);
		if (!map.containsNode("doAuth")) map.insertNode("doAuth", "true", "if true, will request username/password, and enforce login limits");
		if (!map.containsNode("auth")) map.insertNode("auth", "", "format: username:password,username2:password2");
		if (!map.getNode("doAuth").getValue().equals("true")) {
			auth = null;
		}else {
			auth = (map.getNode("auth").getValue()).split(",");
		}
	}
	
	public void setup(ServerSocket s) {
		ComServer server = new ComServer(s, auth);
		server.start();
	}
}
