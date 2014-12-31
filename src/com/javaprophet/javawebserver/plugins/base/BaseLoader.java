package com.javaprophet.javawebserver.plugins.base;

import com.javaprophet.javawebserver.http.Method;
import com.javaprophet.javawebserver.plugins.PatchRegistry;

public class BaseLoader {
	public static void loadBases() {
		// methods
		PatchGetPostHead getposthead = new PatchGetPostHead("getposthead");
		PatchRegistry.registerPatch(getposthead);
		PatchRegistry.registerMethod(Method.GET, getposthead);
		PatchRegistry.registerMethod(Method.POST, getposthead);
		PatchRegistry.registerMethod(Method.HEAD, getposthead);
		// server side languages
		PatchRegistry.registerPatch(new PatchPHP("PHP"));
		// header manipulation
		PatchRegistry.registerPatch(new PatchETag("ETag"));
		// transfer/encoding
		PatchRegistry.registerPatch(new PatchGZip("GZip"));
		
		// special
		PatchRegistry.registerPatch(new PatchContentLength("ContentLength"));
	}
}
