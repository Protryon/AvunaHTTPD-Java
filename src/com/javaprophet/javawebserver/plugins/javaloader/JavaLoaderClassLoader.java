package com.javaprophet.javawebserver.plugins.javaloader;

import java.util.HashMap;

public class JavaLoaderClassLoader extends ClassLoader {
	public JavaLoaderClassLoader() {
		super();
	}
	
	HashMap<String, Class<? extends JavaLoader>> javaLoaders = new HashMap<String, Class<? extends JavaLoader>>();
	
	public String addClass(byte[] data) {
		Class<?> cls = defineClass(data, 0, data.length);
		javaLoaders.put(cls.getName(), (Class<? extends JavaLoader>)cls);
		return cls.getName();
	}
	
	public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		Class<?> see = super.loadClass(name, resolve);
		if (see != null) return see;
		System.out.println(name);
		if (javaLoaders.containsKey(name)) return javaLoaders.get(name);
		return null;
	}
}
