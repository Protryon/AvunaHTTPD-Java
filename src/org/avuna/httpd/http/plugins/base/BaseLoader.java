/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins.base;

import java.io.File;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.http.plugins.Plugin;
import org.avuna.httpd.http.plugins.PluginClassLoader;
import org.avuna.httpd.http.plugins.PluginRegistry;
import org.avuna.httpd.http.plugins.avunaagent.PluginAvunaAgent;
import org.avuna.httpd.http.plugins.base.fcgi.PluginFCGI;
import org.avuna.httpd.http.plugins.security.PluginSecurity;
import org.avuna.httpd.http.plugins.servlet.PluginServlet;
import org.avuna.httpd.http.plugins.ssi.PluginSSI;

public class BaseLoader {
	public static void loadSecBase(PluginRegistry registry) {
		// sec
		registry.registerPatch(new PluginSecurity("Security", registry, new File(registry.getPlugins(), "Security")));
	}
	
	public static void loadBases(PluginRegistry registry) {
		// sec
		loadSecBase(registry);
		registry.registerPatch(new PluginOverride("Override", registry, new File(registry.getPlugins(), "Override")));
		// special
		// PatchRegistry.registerPatch(new PatchEnforceRedirect("EnforceRedirect")); deprecated
		// PatchRegistry.registerPatch(new PatchMultiHost("MultiHost")); deprecated
		registry.registerPatch(new PluginContentType("ContentType", registry, new File(registry.getPlugins(), "ContentType")));
		registry.registerPatch(new PluginCacheControl("CacheControl", registry, new File(registry.getPlugins(), "CacheControl")));
		// methods
		registry.registerPatch(new PluginGetPostHead("GetPostHead", registry, new File(registry.getPlugins(), "GetPostHead")));
		
		registry.registerPatch(new PluginAuth("Auth", registry, new File(registry.getPlugins(), "Auth")));
		
		// server side languages
		registry.registerPatch(new PluginAvunaAgent("AvunaAgent", registry, new File(registry.getPlugins(), "AvunaAgent")));
		registry.registerPatch(new PluginServlet("Servlet", registry, new File(registry.getPlugins(), "Servlet")));
		
		registry.registerPatch(new PluginFCGI("FCGI", registry, new File(registry.getPlugins(), "FCGI")));
		registry.registerPatch(new PluginCGI("CGI", registry, new File(registry.getPlugins(), "CGI")));
		registry.registerPatch(new PluginSCGI("SCGI", registry, new File(registry.getPlugins(), "SCGI")));
		
		registry.registerPatch(new PluginSSI("SSI", registry, new File(registry.getPlugins(), "SSI")));
		
		registry.registerPatch(new PluginInline("Inline", registry, new File(registry.getPlugins(), "Inline")));
		// caching
		registry.registerPatch(new PluginETag("ETag", registry, new File(registry.getPlugins(), "ETag")));
		
		// transfer/encoding
		registry.registerPatch(new PluginGZip("GZip", registry, new File(registry.getPlugins(), "GZip")));
		
		registry.registerPatch(new PluginChunked("Chunked", registry, new File(registry.getPlugins(), "Chunked")));
		
		// special
		registry.registerPatch(new PluginContentLength("ContentLength", registry, new File(registry.getPlugins(), "ContentLength")));
		
	}
	
	public static void loadCustoms(HostHTTP host, File plugins) {
		host.pcl = new PluginClassLoader();
		host.pcl.loadPlugins(host, plugins);
	}
	
	public static void loadCustoms(HostHTTP host, PluginRegistry registry, File plugins) {
		for (Class<? extends Plugin> patchClass : host.customPlugins)
			try {
				registry.registerPatch((Plugin) patchClass.getDeclaredConstructor(String.class).newInstance(patchClass.getName().substring(patchClass.getName().lastIndexOf(".") + 1)));
			}catch (Exception e) {
				host.logger.logError(e);
				host.logger.log("Failed to load plugin: " + patchClass.getName());
			}
	}
}
