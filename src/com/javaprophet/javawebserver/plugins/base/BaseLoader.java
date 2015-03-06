package com.javaprophet.javawebserver.plugins.base;

import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.plugins.PatchClassLoader;
import com.javaprophet.javawebserver.plugins.PatchRegistry;
import com.javaprophet.javawebserver.plugins.javaloader.PatchJavaLoader;

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
		PatchRegistry.registerPatch(new PatchPHP("PHP"));
		
		// caching
		PatchRegistry.registerPatch(new PatchETag("ETag"));
		
		// transfer/encoding
		PatchRegistry.registerPatch(new PatchGZip("GZip"));
		
		PatchRegistry.registerPatch(new PatchChunked("Chunked"));
		
		// special
		PatchRegistry.registerPatch(new PatchContentLength("ContentLength"));
		
		PatchClassLoader pcl = new PatchClassLoader();
		pcl.loadPlugins(JavaWebServer.fileManager.getPlugins());
	}
}
