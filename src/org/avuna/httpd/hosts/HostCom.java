/*	Avuna HTTPD - General Server Applications
    Copyright (C) 2015 Maxwell Bruce

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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
