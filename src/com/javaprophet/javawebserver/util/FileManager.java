package com.javaprophet.javawebserver.util;

import java.io.File;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.plugins.Patch;

public class FileManager {
	public FileManager() {
		
	}
	
	public File getMainDir() {
		return new File((String)JavaWebServer.mainConfig.get("dir"));
	}
	
	public File getHTDocs() {
		return new File(getMainDir(), (String)JavaWebServer.mainConfig.get("htdocs"));
	}
	
	public File getPlugins() {
		return new File(getMainDir(), (String)JavaWebServer.mainConfig.get("plugins"));
	}
	
	public File getTemp() {
		return new File(getMainDir(), (String)JavaWebServer.mainConfig.get("temp"));
	}
	
	public File getPlugin(Patch p) {
		return new File(getPlugins(), p.name);
	}
	
	public File getBaseFile(String name) {
		return new File(getMainDir(), name);
	}
}
