package com.javaprophet.javawebserver.plugins.javaloader;

import java.util.HashMap;

public class JavaLoaderClassLoader extends ClassLoader {
	public JavaLoaderClassLoader() {
		super();
	}
	
	HashMap<String, Class<?>> javaLoaders = new HashMap<String, Class<?>>();
	
	public String addClass(byte[] data) {
		try {
			Class<?> cls = defineClass(data, 0, data.length);
			javaLoaders.put(cls.getName(), cls);
			return cls.getName();
		}catch (NoClassDefFoundError e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public Class<?> loadClass(String name, boolean resolve) {
		if (javaLoaders.containsKey(name)) return javaLoaders.get(name);
		try {
			Class<?> see = super.loadClass(name, resolve);
			if (see != null) return see;
		}catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
}
