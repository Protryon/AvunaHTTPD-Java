package com.javaprophet.javawebserver.plugins.base;

import java.util.HashMap;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.networking.packets.Packet;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.Patch;
import com.javaprophet.javawebserver.plugins.javaloader.JavaLoaderSecurity;
import com.javaprophet.javawebserver.plugins.javaloader.PatchJavaLoader;
import com.javaprophet.javawebserver.plugins.javaloader.security.JLSBFlood;
import com.javaprophet.javawebserver.plugins.javaloader.security.JLSBProxy;
import com.javaprophet.javawebserver.plugins.javaloader.security.JLSFlood;
import com.javaprophet.javawebserver.plugins.javaloader.security.JLSUA;

public class PatchSecurity extends Patch {
	
	public PatchSecurity(String name) {
		super(name);
	}
	
	public void loadBases(PatchJavaLoader pjl) {
		pjl.loadBaseSecurity(new JLSBFlood());
		pjl.loadBaseSecurity(new JLSBProxy());
		pjl.loadBaseSecurity(new JLSFlood());
		pjl.loadBaseSecurity(new JLSUA());
	}
	
	private int minDrop = 100;
	
	@Override
	public void formatConfig(HashMap<String, Object> map) {
		if (!map.containsKey("minDrop")) map.put("minDrop", "100");
		minDrop = Integer.parseInt((String)map.get("minDrop"));
	}
	
	@Override
	public boolean shouldProcessPacket(Packet packet) {
		if (!(packet instanceof RequestPacket)) return false;
		if (((RequestPacket)packet).parent != null) return false;
		if (PatchJavaLoader.security == null || PatchJavaLoader.security.size() < 1) return false;
		return true;
	}
	
	@Override
	public void processPacket(Packet packet) {
		RequestPacket req = (RequestPacket)packet;
		int chance = 0;
		for (JavaLoaderSecurity sec : PatchJavaLoader.security) {
			chance += sec.check(req);
		}
		if (chance >= minDrop) {
			req.drop = true;
			JavaWebServer.bannedIPs.add(req.userIP);
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
