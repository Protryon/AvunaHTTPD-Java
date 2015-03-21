package org.avuna.httpd.plugins;

import java.util.ArrayList;
import java.util.HashMap;
import org.avuna.httpd.http.Method;
import org.avuna.httpd.util.Logger;

public class PatchRegistry {
	
	public static void registerPatch(Patch p) {
		Logger.log("Loading patch " + p.name);
		patchs.add(p);
	}
	
	protected static final HashMap<Method, Patch> registeredMethods = new HashMap<Method, Patch>();
	protected static final ArrayList<Patch> patchs = new ArrayList<Patch>();
	
	public static void registerMethod(Method m, Patch patch) {
		registeredMethods.put(m, patch);
	}
	
	public static Patch getPatchForClass(Class<?> cls) {
		for (Patch p : patchs) {
			if (cls.isAssignableFrom(p.getClass())) {
				return p;
			}
		}
		return null;
	}
}
