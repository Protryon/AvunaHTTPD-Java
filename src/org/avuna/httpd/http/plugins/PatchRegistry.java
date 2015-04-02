package org.avuna.httpd.http.plugins;

import java.util.ArrayList;
import java.util.HashMap;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.http.Method;
import org.avuna.httpd.util.Logger;

public class PatchRegistry {
	public final HostHTTP host;
	
	public PatchRegistry(HostHTTP host) {
		this.host = host;
	}
	
	public void registerPatch(Patch p) {
		if (!p.pcfg.containsKey("enabled") || !p.pcfg.get("enabled").equals("true")) return;
		Logger.log("Loading patch " + p.name);
		patchs.add(p);
	}
	
	protected final HashMap<Method, Patch> registeredMethods = new HashMap<Method, Patch>();
	protected final ArrayList<Patch> patchs = new ArrayList<Patch>();
	
	public void registerMethod(Method m, Patch patch) {
		registeredMethods.put(m, patch);
	}
	
	public Patch getPatchForClass(Class<?> cls) {
		for (Patch p : patchs) {
			if (cls.isAssignableFrom(p.getClass())) {
				return p;
			}
		}
		return null;
	}
}
