package org.avuna.httpd.http.plugins.base;

import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.http.plugins.PatchClassLoader;
import org.avuna.httpd.http.plugins.PatchRegistry;
import org.avuna.httpd.http.plugins.javaloader.PatchJavaLoader;

public class BaseLoader {
	public static void loadBases() {
		// sec
		PatchRegistry.registerPatch(new PatchSecurity("Security"));
		PatchRegistry.registerPatch(new PatchOverride("Override"));
		// special
		// PatchRegistry.registerPatch(new PatchEnforceRedirect("EnforceRedirect")); deprecated
		// PatchRegistry.registerPatch(new PatchMultiHost("MultiHost")); deprecated
		PatchRegistry.registerPatch(new PatchContentType("ContentType"));
		PatchRegistry.registerPatch(new PatchCacheControl("CacheControl"));
		// methods
		PatchRegistry.registerPatch(new PatchGetPostHead("GetPostHead"));
		
		PatchRegistry.registerPatch(new PatchAuth("Auth"));
		
		// server side languages
		PatchRegistry.registerPatch(new PatchJavaLoader("JavaLoader"));
		// PatchRegistry.registerPatch(new PatchJWSL("JWSL")); deprecated
		PatchRegistry.registerPatch(new PatchFCGI("FCGI"));
		
		PatchRegistry.registerPatch(new PatchInline("Inline"));
		// caching
		PatchRegistry.registerPatch(new PatchETag("ETag"));
		
		// transfer/encoding
		PatchRegistry.registerPatch(new PatchGZip("GZip"));
		
		PatchRegistry.registerPatch(new PatchChunked("Chunked"));
		
		// special
		PatchRegistry.registerPatch(new PatchContentLength("ContentLength"));
		
	}
	
	public static void loadCustoms() {
		PatchClassLoader pcl = new PatchClassLoader();
		pcl.loadPlugins(AvunaHTTPD.fileManager.getPlugins());
	}
}
