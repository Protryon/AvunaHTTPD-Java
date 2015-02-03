package com.javaprophet.javawebserver.plugins.base;

import java.util.HashMap;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.networking.Packet;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.Patch;

public class PatchSecurity extends Patch {
	
	public PatchSecurity(String name) {
		super(name);
	}
	
	private static HashMap<String, String> sec = null;
	
	@Override
	public void formatConfig(HashMap<String, Object> map) {
		sec = new HashMap<String, String>();
		for (String mk : map.keySet()) {
			if (mk.equals("enabled")) continue;
			HashMap<String, Object> sub = (HashMap<String, Object>)map.get(mk);
			if (!sub.containsKey("regex")) sub.put("regex", "/\\?[0-9]+");
			if (!sub.containsKey("action")) sub.put("action", "drop");
			sec.put((String)sub.get("regex"), (String)sub.get("action"));
		}
	}
	
	@Override
	public boolean shouldProcessPacket(Packet packet) {
		if (!(packet instanceof RequestPacket)) return false;
		if (sec == null || sec.size() < 1) return false;
		return true;
	}
	
	@Override
	public void processPacket(Packet packet) {
		RequestPacket req = (RequestPacket)packet;
		for (String regex : sec.keySet()) {
			if (req.target.matches(regex)) {
				String action = sec.get(regex);
				if (action.equals("drop")) {
					JavaWebServer.bannedIPs.add(req.userIP);
					req.drop = true;
				}
			}
		}
	}
	
	@Override
	public void processMethod(RequestPacket request, ResponsePacket response) {
		
	}
	
	@Override
	public boolean shouldProcessResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		return false;
	}
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		return data;
	}
	
}
