package org.avuna.httpd.http.plugins;

import java.io.File;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.event.Event;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.event.IEventReceiver;
import org.avuna.httpd.util.Config;
import org.avuna.httpd.util.ConfigFormat;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.Logger;

public abstract class Plugin implements IEventReceiver {
	
	public final String name;
	public final PluginRegistry registry;
	
	public abstract void receive(EventBus bus, Event event);
	
	public abstract void register(EventBus bus);
	
	public void formatConfig(ConfigNode map) {
		if (!map.containsNode("enabled")) map.insertNode("enabled", "true");
	}
	
	public Plugin(String name, PluginRegistry registry) {
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
}
