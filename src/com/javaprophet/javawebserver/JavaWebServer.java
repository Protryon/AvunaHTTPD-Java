package com.javaprophet.javawebserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import org.json.simple.JSONObject;
import com.javaprophet.javawebserver.networking.Connection;
import com.javaprophet.javawebserver.plugins.PluginBus;
import com.javaprophet.javawebserver.plugins.base.BaseLoader;
import com.javaprophet.javawebserver.util.Config;
import com.javaprophet.javawebserver.util.ConfigFormat;
import com.javaprophet.javawebserver.util.FileManager;

public class JavaWebServer {
	public static final String VERSION = "1.0";
	public static Config mainConfig;
	public static ArrayList<Connection> runningThreads = new ArrayList<Connection>();
	public static final FileManager fileManager = new FileManager();
	public static final PluginBus pluginBus = new PluginBus();
	
	public static void setupFolders() {
		fileManager.getMainDir().mkdirs();
		fileManager.getHTDocs().mkdirs();
		fileManager.getPlugins().mkdirs();
		pluginBus.setupFolders();
	}
	
	public static void main(String[] args) {
		try {
			System.out.println("Loading Configs");
			mainConfig = new Config(new File("C:\\jws\\main.cfg"), new ConfigFormat() {
				public void format(JSONObject json) {
					if (!json.containsKey("version")) json.put("version", JavaWebServer.VERSION);
					if (!json.containsKey("dir")) json.put("dir", "C:\\jws");
					if (!json.containsKey("htdocs")) json.put("htdocs", "htdocs");
					if (!json.containsKey("plugins")) json.put("plugins", "plugins");
					if (!json.containsKey("bindport")) json.put("bindport", 80);
					if (!json.containsKey("errorpages")) json.put("errorpages", new JSONObject());
					if (!json.containsKey("index")) json.put("index", "index.html,index.php");
				}
			});
			mainConfig.load();
			setupFolders();
			System.out.println("Loading Base Plugins");
			BaseLoader.loadBases();
			System.out.println("Starting Server");
			ServerSocket server = new ServerSocket(Integer.parseInt(mainConfig.get("bindport").toString()));
			while (!server.isClosed()) {
				Socket s = server.accept();
				DataOutputStream out = new DataOutputStream(s.getOutputStream());
				out.flush();
				DataInputStream in = new DataInputStream(s.getInputStream());
				Connection c = new Connection(s, in, out);
				c.start();
				runningThreads.add(c);
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		pluginBus.preExit();
		if (mainConfig != null) {
			mainConfig.save();
		}
	}
}
