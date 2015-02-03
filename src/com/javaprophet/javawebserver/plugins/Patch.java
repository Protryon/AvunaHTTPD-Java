package com.javaprophet.javawebserver.plugins;

import java.io.File;
import java.util.HashMap;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.networking.Packet;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.util.Config;
import com.javaprophet.javawebserver.util.ConfigFormat;
import com.javaprophet.javawebserver.util.Logger;

public abstract class Patch {
	
	public final String name;
	
	public abstract void formatConfig(HashMap<String, Object> json);
	
	public boolean enabled = true;
	
	public Patch(String name) {
		this.name = name;
		pcfg = new Config(new File(JavaWebServer.fileManager.getPlugin(this), "plugin.cfg"), new ConfigFormat() {
			public void format(HashMap<String, Object> map) {
				if (!map.containsKey("enabled")) map.put("enabled", enabled);
				formatConfig(map);
			}
		});
		try {
			pcfg.load();
			pcfg.save();
		}catch (Exception e) {
			Logger.logError(e);
		}
		enabled = ((String)pcfg.get("enabled")).equals("true");
	}
	
	public void log(String line) {
		Logger.log(name + ": " + line);
	}
	
	public final Config pcfg;
	
	public String toString() {
		return name;
	}
	
	public void preExit() {
		pcfg.save();
	}
	
	public abstract boolean shouldProcessPacket(Packet packet);
	
	public abstract void processPacket(Packet packet);
	
	public abstract void processMethod(RequestPacket request, ResponsePacket response);
	
	public abstract boolean shouldProcessResponse(ResponsePacket response, RequestPacket request, byte[] data);
	
	public abstract byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data);
}
