package org.avuna.httpd.hosts;

import java.net.ServerSocket;
import java.util.HashMap;
import org.avuna.httpd.com.ComServer;

public class HostCom extends Host {
	
	public HostCom(String name) {
		super(name, Protocol.COM);
	}
	
	private String[] auth = null;
	private String ip = null;
	private int port;
	
	public void formatConfig(HashMap<String, Object> map) {
		if (!map.containsKey("port")) map.put("port", "6049");
		if (!map.containsKey("ip")) map.put("ip", "127.0.0.1");
		super.formatConfig(map);
		if (!map.containsKey("doAuth")) map.put("doAuth", "true");
		if (!map.containsKey("auth")) map.put("auth", "");
		if (!map.get("doAuth").equals("true")) {
			auth = null;
		}else {
			auth = ((String)map.get("auth")).split(",");
		}
		ip = (String)map.get("ip");
		port = Integer.parseInt((String)map.get("port"));
	}
	
	public void setup(ServerSocket s) {
		ComServer server = new ComServer(s, auth);
		server.start();
	}
}
