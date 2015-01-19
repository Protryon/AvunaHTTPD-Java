package com.javaprophet.javawebserver.specialplugin.interfaces;

/**
 * @author Luca
 * 
 * A interface which can define a certain class "<i>Pluggable</i>".<br>
 * This means that the class can be loaded, unloaded, enabled and disabled.
 *
 */
public interface IPluggable {
	
	/**
	 * Set the enabled state to the state argument.
	 * @param state the new state of the pluggable.
	 */
	public void setEnabled(boolean state);
	
	/**
	 * Check if the pluggable is enabled.
	 * @return the state of the pluggable.
	 */
	public boolean isEnabled();
	
	/**
	 * Event that will be triggered when a plugin loads.
	 */
	public void onLoad();
	
	/**
	 * Event that will be triggered when a plugin unloads.
	 */
	public void onUnload();

}
