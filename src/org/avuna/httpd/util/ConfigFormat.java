package org.avuna.httpd.util;


public abstract class ConfigFormat {
	public ConfigFormat() {
		
	}
	
	/**
	 * Method for inserting map block into configuration array.
	 * 
	 * @param map
	 */
	public abstract void format(ConfigNode map);
}
