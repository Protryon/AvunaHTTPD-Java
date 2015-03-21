package org.avuna.httpd.plugins.base;

import java.util.HashMap;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.http.networking.Packet;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.plugins.Patch;
import org.avuna.httpd.plugins.javaloader.JavaLoaderSecurity;
import org.avuna.httpd.plugins.javaloader.PatchJavaLoader;
import org.avuna.httpd.plugins.javaloader.security.JLSBFlood;
import org.avuna.httpd.plugins.javaloader.security.JLSBProxy;
import org.avuna.httpd.plugins.javaloader.security.JLSFlood;
import org.avuna.httpd.plugins.javaloader.security.JLSUA;

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
