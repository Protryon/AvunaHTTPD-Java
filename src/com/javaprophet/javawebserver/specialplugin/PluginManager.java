package com.javaprophet.javawebserver.specialplugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.javaprophet.javawebserver.specialplugin.crypto.CryptoManager;
import com.javaprophet.javawebserver.specialplugin.crypto.ICryptoManager;
import com.javaprophet.javawebserver.specialplugin.crypto.IKeyProvider;
import com.javaprophet.javawebserver.specialplugin.events.EventPluginPreLoad;
import com.javaprophet.javawebserver.specialplugin.events.EventPluginUnload;
import com.javaprophet.javawebserver.specialsql.JDBCPluginDatabase;
import com.javaprophet.javawebserver.specialevent.EventBus;
import com.javaprophet.javawebserver.specialplugin.events.EventPluginLoaded;
import com.javaprophet.javawebserver.specialutil.Configuration;

/**
 * @author lucamasira
 *
 * This class will handle everything that has to do with the plugins.<br>
 */
public class PluginManager {
	
	/**
	 * This list contains all the loaded plugins.
	 */
	private final List<Plugin> LOADED_PLUGINS = new ArrayList<Plugin>();
	
	/**
	 * This map contains the plugin names and indexes inside the arraylist for faster plugin name lookup.<br>
	 * The first type is a string which should be the plugin name.<br>
	 * The second type is an integer which represents the array index of the plugin.<br>
	 * These values should always be correct.
	 */
	private final HashMap<String, Integer> NAME_INDEX = new HashMap<String, Integer>();
	
	/**
	 * Maximum allowed plugins, default is Integer.MAX_VALUE
	 */
	private int maximumPlugins = Integer.MAX_VALUE;
	
	/**
	 * The location where all the plugins are
	 */
	private File pluginFolder = new File("plugins" + File.separator);
	
	/**
	 * The classloader used to dynamically load other jars at runtime.
	 */
	private PluginClassLoader pluginClassLoader = new PluginClassLoader();
	
	private EventBus eventBus;
	
	/**
	 * Constructor that uses its own eventbus.
	 */
	public PluginManager() {
		this(new EventBus());
	}
	
	/**
	 * Constructor with an eventbus
	 * @param eventBus
	 */
	public PluginManager(EventBus eventBus) {
		this.setEventBus(eventBus);
	}
	
	/**
	 * This constructor will set the classloader which is used to add the jars dynamically.
	 * @param classloader the classloaderto use
	 */
	public PluginManager(PluginClassLoader classloader) {
		this.pluginClassLoader = classloader;
	}
	
	/**
	 * This constructor will set the plugin folder which will be used to load plugins from.
	 * @param pluginFolder the folder to load plugins from
	 */
	public PluginManager(File pluginFolder) {
		this.pluginFolder = pluginFolder;
	}
	
	/**
	 * This constructor will set the maximum allowed loaded plugins.<br>
	 * The default value of this is Integer.MAX_VALUE
	 * @param maximumAllowedPlugins the maximum amount of loaded plugins.
	 */
	public PluginManager(int maximumAllowedPlugins) {
		this.maximumPlugins = maximumAllowedPlugins;
	}
	
	/**
	 * Loads a plugin from an URL, this url can be a webserver or whatever.<br>
	 * It may return null when an error occurs such as too many loaded plugins.<br>
	 * It's recommended to do a null check when getting a plugin from this method.
	 * @param url the location of the plugin
	 * @param enable should it enable the plugin after it has been loaded?
	 * @return the loaded plugin.
	 * @throws Exception an exception may be throw when the plugin failed to load.
	 */
	public Plugin loadPluginFromURL(final URL url, boolean enable) throws Exception {
		/*
		 * This method doesnt use the function of the URLClassLoader.
		 * This is because we now can call loadPlugin.
		 */
		
		//download the plugin, the string thing will save it as its actual plugin file name such as TestPlugin.jar
		File tempJar = File.createTempFile(url.getPath().substring(url.getPath().lastIndexOf(File.separator)+1,
				url.getPath().lastIndexOf("")), ".jar");
		
		tempJar.deleteOnExit();
		
		//download from the url into the file
		FileOutputStream writer = new FileOutputStream(tempJar);
		InputStream reader = url.openStream();
		int data = reader.read();
		//read the entire file and write it.
		while(data != -1) {
			writer.write(data);
			data = reader.read();
		}
		
		writer.close();
		
		//do this since we still want to use the enable parameter.
		Plugin loadedPlugin = loadPlugin(tempJar);
		if(enable)
			loadedPlugin.setEnabled(true);
		
		return loadedPlugin;
	}
	
	/**
	 * Load a plugin. It may return null when an error occurs such as too many loaded plugins.<br>
	 * It's recommended to do a null check when getting a plugin from this method.
	 * @param file the path to the jar file which will be loaded.
	 * @return the loaded plugin.
	 * @throws Exception an exception is thrown when a plugin couldn't be loaded correctly.
	 */
	public Plugin loadPlugin(final File file) throws Exception {
		//checks if it is still allowed to load plugins.
		if(LOADED_PLUGINS.size() > getMaximumAllowedPlugins())
			throw new Exception("Too many loaded plugins.");
		
		if(file == null)//file needs to to be not null, couldnt use @NotNull
			throw new NullPointerException();
		
		JarFile jarFile = new JarFile(file);
		JarEntry propertiesFile = jarFile.getJarEntry("plugin.properties");
		
		if(propertiesFile == null)
			throw new IOException("plugin.properties file could not be found");
		
		Configuration pluginConfiguration = new Configuration();
		pluginConfiguration.load(jarFile.getInputStream(propertiesFile));
		
		String mainClassPath = pluginConfiguration.getString(PropertyNames.PLUGIN_MAIN);
		
		EventPluginPreLoad eventPreLoad = new EventPluginPreLoad(
				pluginConfiguration.getProperty(PropertyNames.PLUGIN_NAME), mainClassPath);
		
		if(getEventBus().post(eventPreLoad).isCancelled()) {
			return null;
		}
		
		//check if the plugin works on the current os
		if(pluginConfiguration.containsKey(PropertyNames.PLUGIN_OS)) {
			String os = pluginConfiguration.getString(PropertyNames.PLUGIN_OS);
			if(!System.getProperty("os.name").toLowerCase().contains(os.toLowerCase())) {
				throw new Exception("Cannot load plugin: host OS is not supported!");
			}
		}
		
		//needs to be after the event else it would already load the crypted classes
		if(pluginConfiguration.containsKey(PropertyNames.PLUGIN_CRYPTO_ENABLE)) {
			if(pluginConfiguration.getBoolean(PropertyNames.PLUGIN_CRYPTO_ENABLE)) {
				decryptJar(pluginConfiguration, jarFile);//decrypts all crypted classes.
			}
		}
		
		//loads the jar into the classloader
		pluginClassLoader.addURL(new URL("file:"+file.getAbsolutePath()));
		
		//loads the plugin
		Plugin plugin = pluginClassLoader.loadClass(mainClassPath).asSubclass(Plugin.class).newInstance();
		plugin.setPluginConfiguration(pluginConfiguration);
		plugin.setDataFolder(new File(pluginFolder, plugin.getName() + File.separator));
		plugin.setEventBus(getEventBus());
		//could probably fit in some other method/make nicer.
		if(pluginConfiguration.containsKey(PropertyNames.PLUGIN_SQL_DB_ENABLE)) {
			if(pluginConfiguration.getBoolean(PropertyNames.PLUGIN_SQL_DB_ENABLE)) {
				plugin.setDatabase(new JDBCPluginDatabase(plugin));//could probably make more customizable?
			}
		}
		//check if plugin could be added successfully then add it.
		if(addPlugin(plugin)) {
			plugin.onLoad();
		}
		EventPluginLoaded eventLoaded = new EventPluginLoaded(plugin);
		getEventBus().post(eventLoaded);
		return plugin;
	}
	
	/**
	 * Decrypts all crypted classes
	 * @param pluginConfiguration the plugin configuration
	 * @param jarFile tge jar file to decrypt
	 * @throws Exception an exception that might happend while decrypting
	 */
	private void decryptJar(final Configuration pluginConfiguration, final JarFile jarFile) throws Exception
	{
		if(!pluginConfiguration.containsKey(PropertyNames.PLUGIN_CRYPTO_UID) && pluginConfiguration.containsKey(PropertyNames.PLUGIN_CRYPTO_KEYPROVIDER))
			throw new Exception("One or more required crypto properties missing.");

		//Get the key provider which is later is used for decryption.
		Class<? extends IKeyProvider> providerClass = Class.forName(pluginConfiguration
				.getString(PropertyNames.PLUGIN_CRYPTO_KEYPROVIDER)).asSubclass(IKeyProvider.class);
		
		IKeyProvider keyProvider = providerClass.newInstance();
		byte[][] keys = keyProvider.getKeys(pluginConfiguration.getString(PropertyNames.PLUGIN_CRYPTO_UID));//maybe improve on thisand return something else?
		
		ICryptoManager crypto;
		
		if(pluginConfiguration.containsKey(PropertyNames.PLUGIN_CRYPTO_CRYPTOIMP)) {
			String classPath = pluginConfiguration.getString(PropertyNames.PLUGIN_CRYPTO_CRYPTOIMP);
			crypto = Class.forName(classPath).asSubclass(ICryptoManager.class).newInstance();
			crypto.setKeys(keys);
		} else {
			crypto = new CryptoManager(keys[0], keys[1]);//use default
		}
		
		//now decrypt and load all classes
		Enumeration<JarEntry> entryEnumerator = jarFile.entries();
		while(entryEnumerator.hasMoreElements()) {
			JarEntry entry = entryEnumerator.nextElement();
			
			if(entry.getName().endsWith(".class")) {//make sure it's a class
				//read the class file into an array
				ByteArrayOutputStream classReader = new ByteArrayOutputStream();
				InputStream jarEntryInputStream = jarFile.getInputStream(entry);
				int data = jarEntryInputStream.read();
				
				while(data != -1) {
					classReader.write(data);
					data = jarEntryInputStream.read();
				}
				
				byte[] bytecode = classReader.toByteArray();
				
				if(bytecode[0] == 0xCA)//CAFEBABE, it'll be loaded when needed cause the jar is loaded by the pluginClassLoader.
				{
					classReader.close();
					jarEntryInputStream.close();
					continue;
				}

				byte[] decryptedBytecode = crypto.decrypt(bytecode);
				
				String className = entry.getName().replace("/", "");
				/*remove .class from the end, could've done a replace but this is better.
				 * It's better beccause if you have a package called classloaders in your plugin it'll mess it up.
				 */
				className = className.substring(0, className.length() - 6);
				
				//load class
				pluginClassLoader.defineClassFromBytecode(className, decryptedBytecode);
				classReader.close();
				jarEntryInputStream.close();
			}
		}
	}
	
	/**
	 * THIS METHOD WILL NOT UNLOAD THE JAR! READ THIS
	 * It will set the plugin opbject to null and remove the plugin.<br>
	 * Cba to handle PluginClassLoader arrays so I can actually unload it.
	 * @param plugin the plugin to be unloaded.
	 */
	public void unloadPlugin(Plugin plugin) {		
		EventPluginUnload eventUnload = new EventPluginUnload(plugin);
		if(getEventBus().post(eventUnload).isCancelled()) {
			return;
		}
		plugin.setEnabled(false);
		LOADED_PLUGINS.remove(NAME_INDEX.get(plugin.getName().toLowerCase()));
		plugin = null;
		reindexPlugins();
	}
	
	/**
	 * This method will load all the plugins found in the set folder.
	 * @param enable if this is true it will enable all the plugins after they have been loaded.
	 */
	public void loadPluginsFromFolder(boolean enable) {
		loadPluginsFromFolder(pluginFolder, enable);
	}
	
	/**
	 * Load plugins from a specified folder.
	 * @param folder the folder to load from
	 * @param enable enable plugins after all plugins have loaded.
	 */
	public void loadPluginsFromFolder(final File folder, boolean enable) {
		if(!folder.exists())
			folder.mkdir();
		
		for(File plugin : folder.listFiles()) {
			if(plugin.isFile() && plugin.getAbsolutePath().endsWith(".jar")) {
				try {
					loadPlugin(plugin);
				} catch (Exception e) {
					System.err.println(e.getMessage());//just print the exception message so that we cal still load the others.
				}
			}
		}
		if(enable) {
			for(Plugin plugin : getPlugins()) {
				plugin.setEnabled(true);
			}
		}
	}
	
	/**
	 * Get all the loaded plugins.
	 * @return the loaded plugins.
	 */
	public Plugin[] getPlugins() {
		return LOADED_PLUGINS.toArray(new Plugin[LOADED_PLUGINS.size()]);
	}
	
	/**
	 * Get a plugin by its name.<br>
	 * Not case sensitive.<br>
	 * This method will return null if the plugin couldn't be found.
	 * @param name the name of the plugin
	 * @return the plugin associated with the name.
	 */
	public Plugin getPluginByName(final String name) {
		if(!NAME_INDEX.containsKey(name.toLowerCase()))
			return null;
		
		return LOADED_PLUGINS.get(NAME_INDEX.get(name.toLowerCase()));
	}
	
	/**
	 * This will add a plugin to the loadedplugins list and index the name.
	 * @param plugin the plugin that will be added
	 */
	public boolean addPlugin(final Plugin plugin) {
		if(NAME_INDEX.containsKey(plugin.getName().toLowerCase()))
			throw new RuntimeException("Could not add plugin, a plugin was already added with the same name!");
		
		LOADED_PLUGINS.add(plugin);
		NAME_INDEX.put(plugin.getName().toLowerCase(), LOADED_PLUGINS.size() -1);
		return true;
	}
	
	/**
	 * Completely re-indexes the nameIndex hashmap.
	 */
	private void reindexPlugins() {
		NAME_INDEX.clear();
		for(int x = 0; x < LOADED_PLUGINS.size(); x++)
			NAME_INDEX.put(LOADED_PLUGINS.get(x).getName().toLowerCase(), x-1);
	}
	
	/**
	 * Get the location of the folder where all the plugins are in.
	 * @return the location of the folder where all the plugins are in.
	 */
	public File getPluginFolder() {
		return pluginFolder;
	}
	
	/**
	 * Set the location of the folder which contains all the plugins.
	 * @param pluginDir the location of the folder that contains all plugins.
	 */
	public void setPluginFolder(final File pluginDir) {
		pluginFolder = pluginDir;
	}
	
	/**
	 * Get the maximum allowed loaded plugins.
	 * @return the maximum allowed loaded plugins.s
	 */
	public int getMaximumAllowedPlugins() {
		return maximumPlugins;
	}
	
	/**
	 * Set the maximum allowed loaded plugins.
	 * @param maximumAllowed the maximum allowed loaded plugins.
	 */
	public void setMaximumAllowedPlugins(int maximumAllowed) {
		maximumPlugins = maximumAllowed;
	}
	
	/**
	 * Get the custom URLClassLoader which is used to load jars dynamically.
	 * @return the classloader used to load jars dynamically
	 */
	public PluginClassLoader getPluginClassLoader() {
		return pluginClassLoader;
	}
	
	/**
	 * Set the classloader that will be used to load jars dynamically.
	 * @param pluginClassLoader the classloader to use
	 */
	public void setPluginClassLoader(PluginClassLoader pluginClassLoader) {
		this.pluginClassLoader = pluginClassLoader;
	}
	
	/**
	 * Get the eventbus all plugins will use.
	 * @return
	 */
	public EventBus getEventBus() {
		return eventBus;
	}

	/**
	 * Set the eventbus all plugins will use.
	 * @param eventBus
	 */
	public void setEventBus(EventBus eventBus) {
		this.eventBus = eventBus;
	}

	/**
	 * @author Luca
	 * Inner class which contains a bunch of strings like "plugin.name"
	 */
	public static final class PropertyNames {

		/**
		 * Property which defines the plugin's class which extends to Plugin.
		 */
		public static final String PLUGIN_MAIN = "plugin.main";
		
		/**
		 * Property to define the os to run this on, should only be used when JNI is involved.
		 */
		public static final String PLUGIN_OS = "plugin.os";
		
		/**
		 * Property that defines the plugin's name
		 */
		public static final String PLUGIN_NAME = "plugin.name";
		
		/**
		 * Property which defines if crypto is enabled.
		 */
		public static final String PLUGIN_CRYPTO_ENABLE = "plugin.crypto.enable";
		
		/**
		 * Property which defines the plugin's uid used to get the crypto keys.
		 */
		public static final String PLUGIN_CRYPTO_UID = "plugin.crypto.uid";
		
		/**
		 * Property which defines what keyprovider to use.<br>
		 * A keyprovider is used to provide keys for the crypto.<br>
		 */
		public static final String PLUGIN_CRYPTO_KEYPROVIDER = "plugin.crypto.keyprovider";
		
		/**
		 * Classpath of  a class which implements ICryptoManager which can be used for the crypto.
		 */
		public static final String PLUGIN_CRYPTO_CRYPTOIMP = "plugin.crypto.cryptoimp";
		
		/**
		 * Property which defines if SQL is enabled for a certain plugin.
		 */
		public static final String PLUGIN_SQL_DB_ENABLE = "plugin.sql.enable";
		
		/**
		 * Property which defines database name.
		 */
		public static final String PLUGIN_SQL_DB_NAME = "plugin.sql.name";
		
	}
	
	

}
