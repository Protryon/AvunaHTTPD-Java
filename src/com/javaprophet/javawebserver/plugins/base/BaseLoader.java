package com.javaprophet.javawebserver.plugins.base;

import com.javaprophet.javawebserver.plugins.PluginRegistry;

public class BaseLoader {
	public static void loadBases() {
		// server side languages
		PluginRegistry.registerPatch(new PatchPHP("PHP"));
		// header manipulation
		PluginRegistry.registerPatch(new PatchETag("ETag"));
		// transfer/encoding
		PluginRegistry.registerPatch(new PatchGZip("GZip"));
		
		// special
		PluginRegistry.registerPatch(new PatchContentLength("ContentLength"));
	}
}
