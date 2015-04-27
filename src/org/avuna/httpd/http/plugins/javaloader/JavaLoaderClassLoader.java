package org.avuna.httpd.http.plugins.javaloader;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import org.avuna.httpd.util.Logger;

public class JavaLoaderClassLoader extends URLClassLoader {
	
	public JavaLoaderClassLoader(URL[] url, ClassLoader parent) {
		super(url, parent);
		// try {
		// InputStream in = JavaLoaderClassLoader.class.getResourceAsStream("JavaLoader.class");
		// ByteArrayOutputStream tout = new ByteArrayOutputStream();
		// int i = 1;
		// byte[] buf = new byte[1024];
		// while (i > 0) {
		// i = in.read(buf);
		// if (i > 0) tout.write(buf, 0, i);
		// }
		// addClass(tout.toByteArray());
		// in = JavaLoaderClassLoader.class.getResourceAsStream("JavaLoaderPrint.class");
		// tout.reset();
		// i = 1;
		// while (i > 0) {
		// i = in.read(buf);
		// if (i > 0) tout.write(buf, 0, i);
		// }
		// addClass(tout.toByteArray());
		// }catch (IOException e) {
		// Logger.logError(e);
		// }
	}
	
	private HashMap<String, Class<?>> javaLoaders = new HashMap<String, Class<?>>();
	
	public String addClass(byte[] data) throws LinkageError {
		Class<?> cls = defineClass(null, data, 0, data.length);
		javaLoaders.put(cls.getName(), cls);
		return cls.getName();
	}
	
	public Class<?> findClass(String name) {
		if (javaLoaders.containsKey(name)) return javaLoaders.get(name);
		try {
			Class<?> see = super.findClass(name);
			if (see != null) return see;
		}catch (ClassNotFoundException e) {
			Logger.logError(e);
		}
		return null;
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
	
	public void finalize() throws Throwable {
		super.finalize();
		javaLoaders = null;
	}
}
