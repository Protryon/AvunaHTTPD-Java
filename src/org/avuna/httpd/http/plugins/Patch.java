package org.avuna.httpd.http.plugins;

import java.io.File;
import java.io.IOException;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.http.networking.Packet;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.util.Config;
import org.avuna.httpd.util.ConfigFormat;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.Logger;

public abstract class Patch {
	
	public final String name;
	public final PatchRegistry registry;
	
	public void formatConfig(ConfigNode map) {
		if (!map.containsNode("enabled")) map.insertNode("enabled", "true");
	}
	
	public void load() {
		
	}
	
	public void postload() {
		
	}
	
	public Patch(String name, PatchRegistry registry) {
		this.name = name;
		this.registry = registry;
		pcfg = new Config(name, new File(AvunaHTTPD.fileManager.getPlugin(this), "plugin.cfg"), new ConfigFormat() {
			public void format(ConfigNode map) {
				formatConfig(map);
			}
		});
		try {
			pcfg.load();
			pcfg.save();
		}catch (Exception e) {
			Logger.logError(e);
		}
	}
	
	public File getDirectory() {
		return AvunaHTTPD.fileManager.getPlugin(this);
	}
	
	public void log(String line) {
		Logger.log(name + ": " + line);
	}
	
	public final Config pcfg;
	
	public String toString() {
		return name;
	}
	
	public void preExit() {
	}
	
	public void reload() throws IOException {
		pcfg.load();
		pcfg.save();
	}
	
	public abstract boolean shouldProcessPacket(Packet packet);
	
	public abstract void processPacket(Packet packet);
	
	public abstract void processMethod(RequestPacket request, ResponsePacket response);
	
	public abstract boolean shouldProcessResponse(ResponsePacket response, RequestPacket request, byte[] data);
	
	public abstract byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data);
}
