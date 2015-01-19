package com.javaprophet.javawebserver.specialplugin.events;

import com.javaprophet.javawebserver.specialevent.types.EventCancellable;

/**
 * @author lucamasira
 * This event gets fired when a plugin gets loaded. This is before it's loaded by the classloader.
 */
public class EventPluginPreLoad extends EventCancellable {
	
	/**
	 * The name of the plugin that is going to be loaded.
	 */
	private final String name;
	
	/**
	 * The main class of the plugin.
	 */
	private final String mainClass;
	
	/**
	 * Constructor having the two values as arguments.
	 * @param name the name of the plugin that is going to be loaded
	 * @param mainclass the main class name of the plugin.
	 */
	public EventPluginPreLoad(final String name, final String mainClass) {
		this.name = name;
		this.mainClass = mainClass;
	}
	
	/**
	 * Get the name of the plugin that is going to be loaded.
	 * @return the name of the plugin
	 */
	public final String getName() {
		return name;
	}
	
	/**
	 * Get the main plugin class name of the plugin.
	 * @return the main plugin class name.
	 */
	public final String getMainClass() {
		return mainClass;
	}

}
