package com.javaprophet.javawebserver.plugins.base;

import com.javaprophet.javawebserver.plugins.PluginRegistry;

public class BaseLoader {
	public static void loadBases() {
		PluginRegistry.registerPatch(new PatchPHP("PHP"));
	}
}
