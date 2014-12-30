package com.javaprophet.javawebserver.plugins;

import java.io.File;
import java.util.ArrayList;
import org.json.simple.JSONObject;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.http.ContentEncoding;
import com.javaprophet.javawebserver.http.Headers;
import com.javaprophet.javawebserver.networking.Packet;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.util.Config;
import com.javaprophet.javawebserver.util.ConfigFormat;

public abstract class Patch {
	protected static final ArrayList<Patch> patchs = new ArrayList<Patch>();
	
	public final String name;
	
	public abstract void formatConfig(JSONObject json);
	
	public boolean enabled = true;
	
	public Patch(String name) {
		this.name = name;
		pcfg = new Config(new File(JavaWebServer.fileManager.getPlugin(this), "plugin.cfg"), new ConfigFormat() {
			public void format(JSONObject json) {
				formatConfig(json);
			}
		});
		try {
			pcfg.load();
		}catch (Exception e) {
			e.printStackTrace();
		}
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
	
	public abstract boolean shouldProcessResponse(ResponsePacket response, RequestPacket request, Headers headers, ContentEncoding ce, byte[] data);
	
	public abstract byte[] processResponse(ResponsePacket response, RequestPacket request, Headers headers, ContentEncoding ce, byte[] data);
}
