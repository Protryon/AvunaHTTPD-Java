/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins;

import java.io.File;
import org.avuna.httpd.event.Event;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.event.IEventReceiver;
import org.avuna.httpd.util.Config;
import org.avuna.httpd.util.ConfigFormat;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.Logger;

public abstract class Plugin implements IEventReceiver {
	
	public final String name;
	public final File config;
	public final PluginRegistry registry;
	
	public abstract void receive(EventBus bus, Event event);
	
	public abstract void register(EventBus bus);
	
	public void formatConfig(ConfigNode map) {
		if (!map.containsNode("enabled")) map.insertNode("enabled", "true");
	}
	
	public void destroy() {}
	
	public Plugin(String name, PluginRegistry registry, File config) {
		this.name = name;
		this.config = config;
		this.registry = registry;
		pcfg = new Config(name, new File(config, "plugin.cfg"), new ConfigFormat() {
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
	
	public void log(String line) {
		Logger.log(name + ": " + line);
	}
	
	public final Config pcfg;
	
	public String toString() {
		return name;
	}
}
