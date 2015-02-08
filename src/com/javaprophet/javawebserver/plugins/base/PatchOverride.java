package com.javaprophet.javawebserver.plugins.base;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.http.Resource;
import com.javaprophet.javawebserver.networking.Packet;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.Patch;
import com.javaprophet.javawebserver.util.Config;
import com.javaprophet.javawebserver.util.ConfigFormat;
import com.javaprophet.javawebserver.util.Logger;

public class PatchOverride extends Patch {
	
	public PatchOverride(String name) {
		super(name);
		fn = (String)pcfg.get("filename", null);
	}
	
	private String fn = "";
	
	@Override
	public void formatConfig(HashMap<String, Object> json) {
		if (!json.containsKey("filename")) json.put("filename", ".htoverride");
	}
	
	@Override
	public boolean shouldProcessPacket(Packet packet) {
		return false;
	}
	
	@Override
	public void processPacket(Packet packet) {
		
	}
	
	@Override
	public void processMethod(RequestPacket request, ResponsePacket response) {
		
	}
	
	public void reload() throws IOException {
		super.reload();
		nogo.clear();
	}
	
	private ArrayList<String> nogo = new ArrayList<String>();
	private HashMap<String, Config> overrides = new HashMap<String, Config>();
	
	@Override
	public boolean shouldProcessResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		if (response.overrideConfig != null) return true;
		if (response.body == null || response.body.getBody() == null) return false;
		String rt = request.target;
		if (rt.contains("#")) rt = rt.substring(0, rt.indexOf("#"));
		if (rt.contains("?")) rt = rt.substring(0, rt.indexOf("?"));
		if (!request.body.getBody().wasDir) {
			rt = rt.substring(0, rt.lastIndexOf("/") + 1);
		}
		if (overrides.containsKey(rt)) {
			response.overrideConfig = overrides.get(rt);
			return true;
		}
		if (nogo.contains(rt)) return false;
		Resource override = JavaWebServer.fileManager.getResource(rt + fn, response);
		if (override == null) {
			nogo.add(rt);
			return false;
		}
		response.overrideConfig = new Config("override" + System.nanoTime(), new String(override.data), new ConfigFormat() {
			
			@Override
			public void format(HashMap<String, Object> map) {
				
			}
			
		});
		try {
			response.overrideConfig.load();
		}catch (IOException e) {
			Logger.logError(e);
		}
		overrides.put(rt, response.overrideConfig);
		return true;
	}
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		
		return data;
	}
	
}
