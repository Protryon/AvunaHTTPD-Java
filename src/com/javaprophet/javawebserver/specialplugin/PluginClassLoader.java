package com.javaprophet.javawebserver.specialplugin;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author lucamasira
 * This classloader is used to dynamically load jars at runtime without having to use reflection which decreases performance.
 */
public class PluginClassLoader extends URLClassLoader {
	
	/**
	 * Making the constructor simpler.
	 */
	public PluginClassLoader() {
		super(((URLClassLoader)ClassLoader.getSystemClassLoader()).getURLs(), PluginClassLoader.class.getClassLoader());
	}
	
	@Override
	/**
	 * This method will add the jar.<br>
	 * The normal addURL is protected but this will make it public so that we don't require reflection.
	 * @param url the location of the jar that will be added.
	 */
	public void addURL(final URL url) {
		super.addURL(url);
	}
	
	/**
	 * Method that loads a class with a classname and a byte array.<br>
	 * This method is here so no reflection is required which increases performance.
	 * @param name the classname
	 * @param bytecode the bytecode of the class
	 * @return the loaded class
	 */
	public Class<?> defineClassFromBytecode(final String name, final byte[] bytecode) {
		return this.defineClass(name, bytecode, 0, bytecode.length);
	}
}
