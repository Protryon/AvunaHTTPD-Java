package org.avuna.httpd.http.plugins.base;

import java.io.File;
import org.avuna.httpd.http.plugins.PatchClassLoader;
import org.avuna.httpd.http.plugins.PatchRegistry;
import org.avuna.httpd.http.plugins.javaloader.PatchJavaLoader;

public class BaseLoader {
	public static void loadBases(PatchRegistry registry) {
		// sec
		registry.registerPatch(new PatchSecurity("Security", registry));
		registry.registerPatch(new PatchOverride("Override", registry));
		// special
		// PatchRegistry.registerPatch(new PatchEnforceRedirect("EnforceRedirect")); deprecated
		// PatchRegistry.registerPatch(new PatchMultiHost("MultiHost")); deprecated
		registry.registerPatch(new PatchContentType("ContentType", registry));
		registry.registerPatch(new PatchCacheControl("CacheControl", registry));
		// methods
		registry.registerPatch(new PatchGetPostHead("GetPostHead", registry));
		
		registry.registerPatch(new PatchAuth("Auth", registry));
		
		// server side languages
		registry.registerPatch(new PatchJavaLoader("JavaLoader", registry));
		// PatchRegistry.registerPatch(new PatchJWSL("JWSL")); deprecated
		registry.registerPatch(new PatchFCGI("FCGI", registry));
		
		registry.registerPatch(new PatchInline("Inline", registry));
		// caching
		registry.registerPatch(new PatchETag("ETag", registry));
		
		// transfer/encoding
		registry.registerPatch(new PatchGZip("GZip", registry));
		
		registry.registerPatch(new PatchChunked("Chunked", registry));
		
		// special
		registry.registerPatch(new PatchContentLength("ContentLength", registry));
		
	}
	
	public static void loadCustoms(PatchRegistry registry, File plugins) {
		PatchClassLoader pcl = new PatchClassLoader();
		pcl.loadPlugins(registry, plugins);
	}
}
