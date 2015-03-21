package com.javaprophet.javawebserver.hosts;

import java.io.File;
import java.net.ServerSocket;
import java.util.HashMap;
import com.javaprophet.javawebserver.com.ComServer;

public class HostCom extends Host {
	
	public HostCom(String name, String ip, int port, boolean isSSL, File keyFile, String keyPassword, String keystorePassword) {
		super(name, ip, port, isSSL, keyFile, keyPassword, keystorePassword, Protocol.COM);
	}
	
	private String[] auth = null;
	private String ip = null;
	private int port;
	
	public void formatConfig(HashMap<String, Object> map) {
		if (!map.containsKey("doAuth")) map.put("doAuth", "true");
		if (!map.containsKey("auth")) map.put("auth", "admin:jwsisawesome,dev:avunaisbetter");
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
