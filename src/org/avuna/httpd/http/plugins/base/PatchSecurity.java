package org.avuna.httpd.http.plugins.base;

import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.http.networking.Packet;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.Patch;
import org.avuna.httpd.http.plugins.PatchRegistry;
import org.avuna.httpd.http.plugins.javaloader.JavaLoaderSecurity;
import org.avuna.httpd.http.plugins.javaloader.PatchJavaLoader;
import org.avuna.httpd.http.plugins.javaloader.security.JLSConnectionFlood;
import org.avuna.httpd.http.plugins.javaloader.security.JLSGetFlood;
import org.avuna.httpd.http.plugins.javaloader.security.JLSPardon;
import org.avuna.httpd.http.plugins.javaloader.security.JLSPostFlood;
import org.avuna.httpd.http.plugins.javaloader.security.JLSUserAgent;
import org.avuna.httpd.util.ConfigNode;

public class PatchSecurity extends Patch {
	
	public PatchSecurity(String name, PatchRegistry registry) {
		super(name, registry);
	}
	
	public void loadBases(PatchJavaLoader pjl) {
		pjl.loadBaseSecurity(new JLSPardon());
		pjl.loadBaseSecurity(new JLSConnectionFlood());
		pjl.loadBaseSecurity(new JLSPostFlood());
		pjl.loadBaseSecurity(new JLSGetFlood());
		pjl.loadBaseSecurity(new JLSUserAgent());
	}
	
	private int minDrop = 100;
	
	@Override
	public void formatConfig(ConfigNode map) {
		super.formatConfig(map);
		if (!map.containsNode("minDrop")) map.insertNode("minDrop", "100");
		minDrop = Integer.parseInt(map.getNode("minDrop").getValue());
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
			chance += sec.check(req.userIP);
			chance += sec.check(req);
		}
		if (chance >= minDrop) {
			req.drop = true;
			AvunaHTTPD.bannedIPs.add(req.userIP);
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
