/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins.avunaagent;

import java.net.URL;
import java.util.HashMap;
import org.avuna.httpd.hosts.VHost;

public final class AvunaAgentSession {
	private AvunaAgentClassLoader jlcl = null;
	private HashMap<String, String> loadedClasses = new HashMap<String, String>();
	private HashMap<String, AvunaAgent> jls = new HashMap<String, AvunaAgent>();
	private VHost vhost;
	
	public AvunaAgentSession(PluginAvunaAgent pl, VHost vhost, URL[] urls) {
		this.vhost = vhost;
		this.jlcl = new AvunaAgentClassLoader(vhost, urls.clone(), this.getClass().getClassLoader());
		pl.sessions.add(this);
	}
	
	public VHost getVHost() {
		return vhost;
	}
	
	public AvunaAgentClassLoader getJLCL() {
		return jlcl;
	}
	
	public HashMap<String, String> getLoadedClasses() {
		return loadedClasses;
	}
	
	public HashMap<String, AvunaAgent> getJLS() {
		return jls;
	}
	
	public void unloadJLCL() {
		for (AvunaAgent jl : jls.values()) {
			jl.destroy();
		}
		jls = null;
		loadedClasses = null;
		jlcl = null;
		System.gc();
	}
}
