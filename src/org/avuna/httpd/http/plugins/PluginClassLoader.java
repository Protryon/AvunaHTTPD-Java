/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import org.avuna.httpd.hosts.HostHTTP;

public class PluginClassLoader extends ClassLoader {
	public PluginClassLoader() {
		super();
	}
	
	@SuppressWarnings("unchecked")
	public void loadPlugins(HostHTTP host, File dir) {
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				loadPlugins(host, f);
			}else if (f.getName().endsWith(".class")) {
				try {
					FileInputStream fin = new FileInputStream(f);
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					int i = 1;
					byte[] buf = new byte[4096];
					while (i > 0) {
						i = fin.read(buf);
						if (i > 0) {
							bout.write(buf, 0, i);
						}
					}
					fin.close();
					byte[] j = bout.toByteArray();
					@SuppressWarnings("deprecation")
					Class<?> patchClass = defineClass(j, 0, j.length);
					javaLoaders.put(patchClass.getName(), patchClass);
					if (patchClass.isAssignableFrom(Plugin.class)) {
						host.addCustomPlugin((Class<? extends Plugin>) patchClass);
					}
				}catch (Exception e) {
					host.logger.logError(e);
				}
			}
		}
	}
	
	HashMap<String, Class<?>> javaLoaders = new HashMap<String, Class<?>>();
}
