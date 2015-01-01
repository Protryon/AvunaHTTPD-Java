package com.javaprophet.javawebserver.plugins.base;

import com.javaprophet.javawebserver.http.Method;
import com.javaprophet.javawebserver.plugins.PatchRegistry;
import com.javaprophet.javawebserver.plugins.javaloader.PatchJavaLoader;

public class BaseLoader {
	public static void loadBases() {
		// special
		PatchRegistry.registerPatch(new PatchContentType("ContentType"));
		// methods
		PatchGetPostHead getposthead = new PatchGetPostHead("GetPostHead");
		PatchRegistry.registerPatch(getposthead);
		PatchRegistry.registerMethod(Method.GET, getposthead);
		PatchRegistry.registerMethod(Method.POST, getposthead);
		PatchRegistry.registerMethod(Method.HEAD, getposthead);
		// server side languages
		PatchRegistry.registerPatch(new PatchJavaLoader("JavaLoader"));
		PatchRegistry.registerPatch(new PatchJWSL("JWSL"));
		PatchRegistry.registerPatch(new PatchPHP("PHP"));
		// header manipulation
		PatchRegistry.registerPatch(new PatchETag("ETag"));
		// transfer/encoding
		PatchRegistry.registerPatch(new PatchGZip("GZip"));
		
		// special
		PatchRegistry.registerPatch(new PatchContentLength("ContentLength"));
	}
}
