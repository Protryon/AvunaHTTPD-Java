package com.javaprophet.javawebserver.plugins.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import sun.misc.BASE64Decoder;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.http.ResponseGenerator;
import com.javaprophet.javawebserver.http.StatusCode;
import com.javaprophet.javawebserver.networking.packets.Packet;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.Patch;
import com.javaprophet.javawebserver.util.Logger;

public class PatchAuth extends Patch {
	
	public PatchAuth(String name) {
		super(name);
	}
	
	@Override
	public void formatConfig(HashMap<String, Object> map) {
		auths = new ArrayList<Auth>();
		HashMap<String, Object> authList;
		if (!map.containsKey("authList")) {
			map.put("authList", authList = new HashMap<String, Object>());
		}else {
			authList = (HashMap<String, Object>)map.get("authList");
		}
		for (String key : authList.keySet()) {
			HashMap<String, Object> auth = (HashMap<String, Object>)authList.get(key);
			if (!auth.containsKey("userlist")) auth.put("userlist", "users.txt");
			if (!auth.containsKey("cacheUsers")) auth.put("cacheUsers", "true");
			if (!auth.containsKey("dirMatch")) auth.put("dirMatch", ".*");
			if (!auth.containsKey("realm")) auth.put("realm", "Generic Auth Title");
			auths.add(new Auth((String)auth.get("userlist"), ((String)auth.get("cacheUsers")).equals("true"), (String)auth.get("dirMatch"), (String)auth.get("realm")));
		}
		
	}
	
	public void reload() throws IOException {
		super.reload();
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
					File ul = new File(JavaWebServer.fileManager.getPlugin(PatchAuth.this), userlist);
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
								return true;
							}
						}
					}
					scan.close();
					return cr;
				}catch (IOException e) {
					Logger.logError(e);;
				}
			}else {
				return ull.contains(up);
			}
			return false;
		}
	}
	
	private ArrayList<Auth> auths;
	
	@Override
	public boolean shouldProcessPacket(Packet packet) {
		return false;
	}
	
	@Override
	public void processPacket(Packet packet) {
		
	}
	
	@Override
	public boolean shouldProcessResponse(ResponsePacket response, RequestPacket request, byte[] data) {
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
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		Auth auth = null;
		for (Auth tauth : auths) {
			if (tauth.matchesDir(request.target)) {
				auth = tauth;
				break;
			}
		}
		if (auth == null) return null;
		try {
			if (request.headers.hasHeader("Authorization")) {
				String as = request.headers.getHeader("Authorization");
				if (as.startsWith("Basic ")) {
					as = as.substring(6);
					as = new String(new BASE64Decoder().decodeBuffer(as));
					if (auth.isAuth(as)) {
						return data;
					}
				}
			}
			ResponseGenerator.generateDefaultResponse(response, StatusCode.UNAUTHORIZED);
			response.body = null;
			response.headers.addHeader("WWW-Authenticate", "Basic realm=\"" + auth.realm + "\"");
		}catch (Exception e) {
			Logger.logError(e);;
		}
		return null;
	}
	
	@Override
	public void processMethod(RequestPacket request, ResponsePacket response) {
		
	}
}
