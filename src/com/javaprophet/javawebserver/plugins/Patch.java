package com.javaprophet.javawebserver.plugins;

import java.io.File;
import org.json.simple.JSONObject;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.networking.Packet;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.util.Config;
import com.javaprophet.javawebserver.util.ConfigFormat;

public abstract class Patch {
	
	public final String name;
	
	public abstract void formatConfig(JSONObject json);
	
	public boolean enabled = true;
	
	public Patch(String name) {
		this.name = name;
		pcfg = new Config(new File(JavaWebServer.fileManager.getPlugin(this), "plugin.cfg"), new ConfigFormat() {
			public void format(JSONObject json) {
				if (!json.containsKey("enabled")) json.put("enabled", enabled);
				formatConfig(json);
			}
		});
		try {
			pcfg.load();
		}catch (Exception e) {
			e.printStackTrace();
		}
		enabled = (Boolean)pcfg.get("enabled");
	}
	
	public void log(String line) {
		System.out.println(name + ": " + line);
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
