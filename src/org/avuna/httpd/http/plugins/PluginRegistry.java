/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins;

import java.io.File;
import java.util.ArrayList;
import org.avuna.httpd.hosts.VHost;

public class PluginRegistry {
	public final VHost host;
	
	public PluginRegistry(VHost host) {
		this.host = host;
	}
	
	public File getPlugins() {
		return host.getPlugins();
	}
	
	public void registerPatch(Plugin p) {
		if (!p.pcfg.containsNode("enabled") || !p.pcfg.getNode("enabled").getValue().equals("true")) return;
		host.logger.log("Loading patch " + p.name);
		p.register(host.getHost().eventBus); // TODO: vhost event bus?
		patchs.add(p);
	}
	
	public final ArrayList<Plugin> patchs = new ArrayList<Plugin>();
	
	public Plugin getPatchForClass(Class<?> cls) {
		for (Plugin p : patchs) {
			if (cls.isAssignableFrom(p.getClass())) {
				return p;
			}
		}
		return null;
	}
}
