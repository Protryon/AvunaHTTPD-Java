package com.javaprophet.javawebserver.specialsql;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import com.javaprophet.javawebserver.specialplugin.Plugin;
import com.javaprophet.javawebserver.specialplugin.PluginManager.PropertyNames;

/**
 * @author lucamasira
 * Database class for plugins which extends to JDBCDatabase.<br>
 * Only the constructor is required because it uses all methods from the JDBCDatabase class.
 */
public class JDBCPluginDatabase extends JDBCDatabase {
	
	/**
	 * Constructor which requires a plugin object.<br>
	 * From the plugin object it can create and or connect to a database.
	 * @param plugin the plugin object.
	 * @throws IOException an exception that can occur when creating a database file
	 * @throws SQLException an exception that can occur when connecting to the database
	 */
	public JDBCPluginDatabase(final Plugin plugin) throws IOException, SQLException {
		//setup the db/check if it exists
		File dbFile = new File(plugin.getDataFolder(), "data.db");//default db file name
		if(plugin.getPluginConfiguration().containsKey(PropertyNames.PLUGIN_SQL_DB_NAME))
			dbFile = new File(plugin.getDataFolder(), plugin.getPluginConfiguration().getString(PropertyNames.PLUGIN_SQL_DB_NAME));
		
		//create the plugin folder
		if(!plugin.getDataFolder().exists())
			plugin.getDataFolder().mkdirs();
		
		//create the database
		if(!dbFile.exists())
			dbFile.createNewFile();
		
		connect("jdbc:sqlite:" + dbFile.getAbsolutePath());//connects to the database
	}

}
