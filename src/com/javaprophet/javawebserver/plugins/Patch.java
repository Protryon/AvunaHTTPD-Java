package com.javaprophet.javawebserver.plugins;

import java.io.File;
import java.util.ArrayList;
import org.json.simple.JSONObject;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.http.ContentEncoding;
import com.javaprophet.javawebserver.http.Headers;
import com.javaprophet.javawebserver.networking.Packet;
import com.javaprophet.javawebserver.util.Config;
import com.javaprophet.javawebserver.util.ConfigFormat;

public abstract class Patch {
	protected static final ArrayList<Patch> patchs = new ArrayList<Patch>();
	
	public final String name;
	
	public void formatConfig(JSONObject json) {
		
	}
	
	public boolean enabled = true;
	
	public Patch(String name) {
		this.name = name;
		pcfg = new Config(new File(JavaWebServer.fileManager.getPlugin(this), "plugin.cfg"), new ConfigFormat() {
			public void format(JSONObject json) {
				formatConfig(json);
			}
		});
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
	
	public abstract boolean shouldProcessResponse(Headers headers, ContentEncoding ce, boolean data, byte[] response);
	
	public abstract byte[] processResponse(Headers headers, ContentEncoding ce, boolean data, byte[] response);
}
