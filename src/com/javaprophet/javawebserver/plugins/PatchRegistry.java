package com.javaprophet.javawebserver.plugins;

import java.util.ArrayList;
import java.util.HashMap;
import com.javaprophet.javawebserver.http.Method;

public class PatchRegistry {
	
	public static void registerPatch(Patch p) {
		patchs.add(p);
	}
	
	protected static final HashMap<Method, Patch> registeredMethods = new HashMap<Method, Patch>();
	protected static final ArrayList<Patch> patchs = new ArrayList<Patch>();
	
	public static void registerMethod(Method m, Patch patch) {
		registeredMethods.put(m, patch);
	}
}
