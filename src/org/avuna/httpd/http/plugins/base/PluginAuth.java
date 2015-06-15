package org.avuna.httpd.http.plugins.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.event.Event;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.event.base.EventID;
import org.avuna.httpd.event.base.EventReload;
import org.avuna.httpd.http.ResponseGenerator;
import org.avuna.httpd.http.StatusCode;
import org.avuna.httpd.http.event.EventGenerateResponse;
import org.avuna.httpd.http.event.HTTPEventID;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.Plugin;
import org.avuna.httpd.http.plugins.PluginRegistry;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.Logger;
import sun.misc.BASE64Decoder;

public class PluginAuth extends Plugin {
	
	public PluginAuth(String name, PluginRegistry registry) {
		super(name, registry);
	}
	
	@Override
	public void formatConfig(ConfigNode map) {
		super.formatConfig(map);
		auths = new ArrayList<Auth>();
		if (!map.containsNode("authList")) {
			map.insertNode("authList");
		}
		ConfigNode authList = map.getNode("authList");
		for (String key : authList.getSubnodes()) {
			ConfigNode auth = authList.getNode(key);
			if (!auth.containsNode("userlist")) auth.insertNode("userlist", "users.txt");
			if (!auth.containsNode("cacheUsers")) auth.insertNode("cacheUsers", "true");
			if (!auth.containsNode("dirMatch")) auth.insertNode("dirMatch", ".*");
			if (!auth.containsNode("realm")) auth.insertNode("realm", "Generic Auth Title");
			auths.add(new Auth(auth.getNode("userlist").getValue(), (auth.getNode("cacheUsers").getValue()).equals("true"), auth.getNode("dirMatch").getValue(), auth.getNode("realm").getValue()));
		}
		for (Auth a : auths) {
			a.usersLoaded = false;
			a.ull.clear();
		}
	}
	
	private final class Auth {
		public String userlist;
		public boolean cacheUsers;
		public String dirMatch;
		public String realm;
		public boolean usersLoaded = false;
		
		public Auth(String userlist, boolean cacheUsers, String dirMatch, String realm) {
			this.userlist = userlist;
			this.cacheUsers = cacheUsers;
			this.dirMatch = dirMatch;
			this.realm = realm;
		}
		
		public boolean matchesDir(String dir) {
			return dir.matches(dirMatch);
		}
		
		private ArrayList<String> ull = new ArrayList<String>();
		
		public boolean isAuth(String up) {
			if (!cacheUsers || !usersLoaded) {
				try {
					File ul = new File(AvunaHTTPD.fileManager.getPlugin(PluginAuth.this), userlist);
					if (!ul.exists()) ul.createNewFile();
					Scanner scan = new Scanner(new FileInputStream(ul));
					boolean cr = false;
					while (scan.hasNextLine()) {
						String line = scan.nextLine().trim();
						if (cacheUsers) {
							ull.add(line);
							if (line.equals(up)) {
								cr = true;
							}
						}else {
							if (line.equals(up)) {
								scan.close();
								return true;
							}
						}
					}
					scan.close();
					return cr;
				}catch (IOException e) {
					Logger.logError(e);
				}
			}else {
				return ull.contains(up);
			}
			return false;
		}
	}
	
	private ArrayList<Auth> auths;
	
	public boolean shouldProcessResponse(ResponsePacket response, RequestPacket request) {
		if (request.parent != null) return false;
		if (response.body != null) {
			for (Auth auth : auths) {
				if (auth.matchesDir(request.target)) {
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public void receive(EventBus bus, Event event) {
		if (event instanceof EventGenerateResponse) {
			EventGenerateResponse egr = (EventGenerateResponse)event;
			ResponsePacket response = egr.getResponse();
			RequestPacket request = egr.getRequest();
			if (!shouldProcessResponse(response, request)) return;
			Auth auth = null;
			for (Auth tauth : auths) {
				if (tauth.matchesDir(request.target)) {
					auth = tauth;
					break;
				}
			}
			if (auth == null) {
				ResponseGenerator.generateDefaultResponse(response, StatusCode.UNAUTHORIZED);
				response.body = null;
				return;
			}
			try {
				if (request.headers.hasHeader("Authorization")) {
					String as = request.headers.getHeader("Authorization");
					if (as.startsWith("Basic ")) {
						as = as.substring(6);
						as = new String(new BASE64Decoder().decodeBuffer(as));
						if (auth.isAuth(as)) {
							return;
						}
					}
				}
				ResponseGenerator.generateDefaultResponse(response, StatusCode.UNAUTHORIZED);
				response.body = null;
				response.headers.addHeader("WWW-Authenticate", "Basic realm=\"" + auth.realm + "\"");
			}catch (Exception e) {
				Logger.logError(e);
			}
		}else if (event instanceof EventReload) {
			// for (Auth a : auths) {
			// a.usersLoaded = false;
			// a.ull.clear();
			// } from format config
		}
	}
	
	@Override
	public void register(EventBus bus) {
		bus.registerEvent(HTTPEventID.GENERATERESPONSE, this, -400);
		bus.registerEvent(EventID.RELOAD, this, 0);
	}
}
