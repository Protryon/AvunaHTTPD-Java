package com.javaprophet.javawebserver.plugins.base;

import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.plugins.PatchClassLoader;
import com.javaprophet.javawebserver.plugins.PatchRegistry;
import com.javaprophet.javawebserver.plugins.javaloader.PatchJavaLoader;

public class BaseLoader {
	public static void loadBases() {
		// special
		PatchRegistry.registerPatch(new PatchEnforceRedirect("EnforceRedirect"));
		PatchRegistry.registerPatch(new PatchMultiHost("MultiHost"));
		PatchRegistry.registerPatch(new PatchContentType("ContentType"));
		// methods
		PatchGetPostHead getposthead = new PatchGetPostHead("GetPostHead");
		PatchRegistry.registerPatch(getposthead);
		
		PatchRegistry.registerPatch(new PatchAuth("Auth"));
		
		// server side languages
		PatchRegistry.registerPatch(new PatchJavaLoader("JavaLoader"));
		PatchRegistry.registerPatch(new PatchJWSL("JWSL"));
		PatchRegistry.registerPatch(new PatchPHP("PHP"));
		
		// caching
		PatchRegistry.registerPatch(new PatchETag("ETag"));
		
		// transfer/encoding
		PatchRegistry.registerPatch(new PatchGZip("GZip"));
		
		// special
		PatchRegistry.registerPatch(new PatchContentLength("ContentLength"));
		
		PatchClassLoader pcl = new PatchClassLoader();
		pcl.loadPlugins(JavaWebServer.fileManager.getPlugins());
	}
}
