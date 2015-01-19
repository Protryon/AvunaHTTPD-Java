package com.javaprophet.javawebserver.specialplugin;

import java.io.File;

import com.javaprophet.javawebserver.specialsql.JDBCPluginDatabase;
import com.javaprophet.javawebserver.specialevent.EventBus;
import com.javaprophet.javawebserver.specialplugin.PluginManager.PropertyNames;
import com.javaprophet.javawebserver.specialplugin.events.EventPluginChangeState;
import com.javaprophet.javawebserver.specialplugin.interfaces.IPluggable;
import com.javaprophet.javawebserver.specialutil.Configuration;

/**
 * @author lucamasira
 * 
 * This class is the base for all the plugins that will be created.<br>
 * Extend to this class to create your own plugin.
 */
public class Plugin implements IPluggable{
	
	/**
	 * The dedicated folder for a plugin.<br>
	 * For example: plugins/testplugin/
	 */
	private File dataFolder;
	
	/**
	 * This Configuration object contains all the plugin information such as the main class, name, version and description.
	 */
	private Configuration pluginConfig;
	
	/**
	 * The state of the plugin which can be enabled/true or disabled/false.
	 */
	private boolean enabled;
	
	/**
	 * Dedicated SQL database.<br>
	 * Didn't define it as an IDatabase since this kind of database is specifically made for plugins.
	 */
	private JDBCPluginDatabase database;
	
	private EventBus eventBus;
	
	
	/**
	 * Normal constructor
	 */
	public Plugin() {	
	}
	
	/**
	 * Event that will be fired when the plugin is loaded.
	 */
	public void onLoad() {
	}
	
	/**
	 * Event that will be fired when a plugin gets unloaded.
	 */
	public void onUnload() {
	}
	
	
	/**
	 * This method will be called when a plugin is enabled.
	 */
	protected void onEnable() {
	}
	
	/**
	 * This method will be called when a plugin is being disabled.
	 */
	protected void onDisable() {
	}
	
	/**
	 * Will get the plugin state.
	 * @return the state of the plugin which can either be enabled/true or disabled/false.
	 */
	public boolean isEnabled() {
		return enabled;
	}
	
	/**
	 * This method will set the plugin to the state set in the state argument.<br>
	 * This method will also prevent double enabled and double disable.
	 * @param state the new plugin state which can be enabled/true or disabled/false.
	 */
	public void setEnabled(boolean state) {
		//is already in the same state, return. Don't fire event before this.
		if(isEnabled() == state)
			return;
		
		EventPluginChangeState eventChange = new EventPluginChangeState(this);
		if(getEventBus().post(eventChange).isCancelled()) {
			return;
		}
		
		//toggle the plugin depending on the new state, also sets the new state.
		if(enabled = state)
			onEnable();
		else
			onDisable();
	}
	
	/**
	 * Toggles the state of the plugin.
	 */
	public void toggle() {
		setEnabled(!isEnabled());
	}
	
	/**
	 * Set the plugin properties.<br>
	 * This is most likely only used by the PluginManager instance.<br>
	 * If the pluginmanager would use reflection it would decrease performance.
	 * @param config the new properties.
	 */
	public void setPluginConfiguration(Configuration config) {
		this.pluginConfig = config;
	}
	
	/**
	 * Set the dedicated folder for the plugin.
	 * @param folder the folder that can be used by the plugin.
	 */
	public void setDataFolder(File folder) {
		this.dataFolder = folder;
	}
	
	/**
	 * This will give the properties about the plugin.
	 * @return the properties about the plugin such as main class, name, version and description.
	 */
	public Configuration getPluginConfiguration() {
		return pluginConfig;
	}
	
	/**
	 * Get the dedicated plugin folder.
	 * @return the dedicated plugin folder.
	 */
	public File getDataFolder() {
		return dataFolder;
	}
	
	/**
	 * Get the database associated with the plugin.<br>
	 * It will <b>return null<br> if no database is defined in plugin.properties<br>
	 * There's also no need to call the connect method since the database will connect when the plugin is loaded.
	 * @return the SQL database for the plugin
	 */
	public JDBCPluginDatabase getDatabase() {
		return database;
	}
	
	/**
	 * Set the database implementation which will be used.
	 * @param database the IPluginDatabase implementation that will be used
	 */
	public void setDatabase(JDBCPluginDatabase database) {
		this.database = database;
	}

	/**
	 * Easier way to get a plugin's name.
	 * @return the plugin name
	 */
	public String getName() {
		return getPluginConfiguration().getString(PropertyNames.PLUGIN_NAME);
	}

	/**
	 * The eventbus used for the plugin.
	 * @return the eventbus
	 */
	public EventBus getEventBus() {
		return eventBus;
	}

	/**
	 * Set the eventbus that will be used for this plugin
	 * @param eventBus the eventbus to use.
	 */
	public void setEventBus(EventBus eventBus) {
		this.eventBus = eventBus;
	}
}
