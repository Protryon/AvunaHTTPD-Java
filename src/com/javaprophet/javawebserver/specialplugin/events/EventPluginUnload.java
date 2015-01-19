package com.javaprophet.javawebserver.specialplugin.events;

import com.javaprophet.javawebserver.specialevent.types.EventCancellable;
import com.javaprophet.javawebserver.specialplugin.Plugin;


/**
 * @author lucamasira
 * This event will be fired when a plugin is being unloaded
 */
public class EventPluginUnload extends EventCancellable {
	
	/**
	 * The plugin that is being unloaded.
	 */
	private final Plugin plugin;
	
	/**
	 * The constructor which takes the plugin as an argument.
	 * @param plugin the plugin that is being unloaded
	 */
	public EventPluginUnload(final Plugin plugin) {
		this.plugin = plugin;
	}
	
	/**
	 * Get the plugin.<br>
	 * @return the plugin that is being unloaded.
	 */
	public final Plugin getPlugin() {
		return plugin;
	}

}
