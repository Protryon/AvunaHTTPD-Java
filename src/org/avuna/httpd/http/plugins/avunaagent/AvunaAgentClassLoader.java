/*
 * Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins.avunaagent;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import org.avuna.httpd.hosts.VHost;

public class AvunaAgentClassLoader extends URLClassLoader {
	private final VHost vhost;
	
	public AvunaAgentClassLoader(VHost vhost, URL[] url, ClassLoader parent) {
		super(url, parent);
		this.vhost = vhost;
	}
	
	private HashMap<String, Class<?>> javaLoaders = new HashMap<String, Class<?>>();
	
	public String addClass(byte[] data) throws LinkageError {
		Class<?> cls = defineClass(null, data, 0, data.length);
		javaLoaders.put(cls.getName(), cls);
		return cls.getName();
	}
	
	public Class<?> findClass(String name) {
		if (javaLoaders.containsKey(name)) return javaLoaders.get(name);
		try {
			Class<?> see = super.findClass(name);
			if (see != null) return see;
		}catch (ClassNotFoundException e) {
			vhost.logger.logError(e);
		}
		return null;
	}
	
	public Class<?> loadClass(String name, boolean resolve) {
		if (javaLoaders.containsKey(name)) return javaLoaders.get(name);
		try {
			Class<?> see = super.loadClass(name, resolve);
			if (see != null) return see;
		}catch (ClassNotFoundException e) {
			vhost.logger.logError(e);
		}
		return null;
	}
	
	public void finalize() throws Throwable {
		super.finalize();
		javaLoaders = null;
	}
}
