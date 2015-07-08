/*
 * Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins.base;

import java.io.File;
import org.avuna.httpd.http.plugins.PluginClassLoader;
import org.avuna.httpd.http.plugins.PluginRegistry;
import org.avuna.httpd.http.plugins.avunaagent.PluginAvunaAgent;
import org.avuna.httpd.http.plugins.security.PluginSecurity;

public class BaseLoader {
	public static void loadSecBase(PluginRegistry registry) {
		// sec
		registry.registerPatch(new PluginSecurity("Security", registry));
	}
	
	public static void loadBases(PluginRegistry registry) {
		// sec
		loadSecBase(registry);
		registry.registerPatch(new PluginOverride("Override", registry));
		// special
		// PatchRegistry.registerPatch(new PatchEnforceRedirect("EnforceRedirect")); deprecated
		// PatchRegistry.registerPatch(new PatchMultiHost("MultiHost")); deprecated
		registry.registerPatch(new PluginContentType("ContentType", registry));
		registry.registerPatch(new PluginCacheControl("CacheControl", registry));
		// methods
		registry.registerPatch(new PluginGetPostHead("GetPostHead", registry));
		
		registry.registerPatch(new PluginAuth("Auth", registry));
		
		// server side languages
		registry.registerPatch(new PluginAvunaAgent("AvunaAgent", registry));
		// PatchRegistry.registerPatch(new PatchJWSL("JWSL")); deprecated
		registry.registerPatch(new PluginFCGI("FCGI", registry));
		
		registry.registerPatch(new PluginInline("Inline", registry));
		// caching
		registry.registerPatch(new PluginETag("ETag", registry));
		
		// transfer/encoding
		registry.registerPatch(new PluginGZip("GZip", registry));
		
		registry.registerPatch(new PluginChunked("Chunked", registry));
		
		// special
		registry.registerPatch(new PluginContentLength("ContentLength", registry));
		
	}
	
	public static void loadCustoms(PluginRegistry registry, File plugins) {
		PluginClassLoader pcl = new PluginClassLoader();
		pcl.loadPlugins(registry, plugins);
	}
}
