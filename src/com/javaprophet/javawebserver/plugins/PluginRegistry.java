package com.javaprophet.javawebserver.plugins;

public class PluginRegistry {
	
	public static void registerPatch(Patch p) {
		Patch.patchs.add(p);
	}
}
