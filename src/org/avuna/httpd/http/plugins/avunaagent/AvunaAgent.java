/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins.avunaagent;

import org.avuna.httpd.hosts.VHost;
import org.avuna.httpd.util.ConfigNode;

public abstract class AvunaAgent {
	public AvunaAgent() {
		
	}
	
	/** Called before (almost) every destruction of an Avuna Agent. Only common case of not being called is possibly when a SIGKILL is sent. */
	public void destroy() {
		
	}
	
	/** Called many times, this should only be used for enforcing entries in the config, input validation, and storing the values for later use. */
	public void formatConfig(ConfigNode cfg) {
		
	}
	
	public ConfigNode pcfg = null;
	public VHost host = null;
	
	/** Should not override the constructor, use this instead, as it is called after things like the Config is loaded. */
	public void init() {}
	
	/** Called after other plugins(when preloading) & Avuna Agents have been loaded. */
	public void postinit() {}
	
	/** Used strictly for defining new JavaLoader types, and casting. */
	public int getType() {
		return -1;
	}
	
}
