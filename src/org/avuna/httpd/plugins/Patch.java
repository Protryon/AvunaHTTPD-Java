package org.avuna.httpd.plugins;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.http.networking.Packet;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.util.Config;
import org.avuna.httpd.util.ConfigFormat;
import org.avuna.httpd.util.Logger;

public abstract class Patch {
	
	public final String name;
	
	public abstract void formatConfig(HashMap<String, Object> json);
	
	public Patch(String name) {
		this.name = name;
		pcfg = new Config(name, new File(AvunaHTTPD.fileManager.getPlugin(this), "plugin.cfg"), new ConfigFormat() {
			public void format(HashMap<String, Object> map) {
				if (!map.containsKey("enabled")) map.put("enabled", "true");
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
