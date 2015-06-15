package org.avuna.httpd.http.plugins.base;

import java.io.File;
import org.avuna.httpd.http.plugins.PluginClassLoader;
import org.avuna.httpd.http.plugins.PluginRegistry;
import org.avuna.httpd.http.plugins.javaloader.PluginJavaLoader;

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
		registry.registerPatch(new PluginJavaLoader("JavaLoader", registry));
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
