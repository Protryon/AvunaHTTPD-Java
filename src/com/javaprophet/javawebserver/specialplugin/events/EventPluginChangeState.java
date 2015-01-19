package com.javaprophet.javawebserver.specialplugin.events;

import com.javaprophet.javawebserver.specialevent.types.EventCancellable;
import com.javaprophet.javawebserver.specialplugin.Plugin;


/**
 * @author lucamasira
 * This event will be fired when the plugin is changing enabled states.
 */
public class EventPluginChangeState extends EventCancellable {
	
	/**
	 * The plugin that is going to change state.
	 */
	private final Plugin plugin;
	
	/**
	 * The constructor which takes the plugin as an argument.
	 * @param plugin the plugin that is going to change states
	 */
	public EventPluginChangeState(final Plugin plugin) {
		this.plugin = plugin;
	}
	
	/**
	 * Get the plugin.<br>
	 * The value of isEnabled in the plugin is the old state, the new state will be the inversed value.
	 * @return the plugin that is going to change states.
	 */
	public final Plugin getPlugin() {
		return plugin;
	}
	
	/**
	 * The new plugin state.
	 * @return the state of the plugin if the event doesn't get cancelled.
	 */
	public final boolean getNewState() {
		return !getPlugin().isEnabled();
	}

}
