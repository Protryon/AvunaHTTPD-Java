package com.javaprophet.javawebserver.plugins.javaloader;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import com.javaprophet.javawebserver.util.Logger;

public class JavaLoaderClassLoader extends URLClassLoader {
	public JavaLoaderClassLoader(URL[] url, ClassLoader parent) {
		super(url);
	}
	
	HashMap<String, Class<?>> javaLoaders = new HashMap<String, Class<?>>();
	
	public String addClass(byte[] data) throws LinkageError {
		Class<?> cls = defineClass(data, 0, data.length);
		javaLoaders.put(cls.getName(), cls);
		return cls.getName();
	}
	
	public Class<?> loadClass(String name, boolean resolve) {
		if (javaLoaders.containsKey(name)) return javaLoaders.get(name);
		try {
			Class<?> see = super.loadClass(name, resolve);
			if (see != null) return see;
		}catch (ClassNotFoundException e) {
			Logger.logError(e);
		}
		return null;
	}
}
