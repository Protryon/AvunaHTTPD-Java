package com.javaprophet.javawebserver.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;

public class Config {
	private final File cfg;
	private ConfigFormat format = null;
	private HashMap<String, Object> cm = new HashMap<String, Object>();
	public final String name;
	private final String iconf;
	public static final HashMap<String, Config> configs = new HashMap<String, Config>();
	
	public Config(String name, File cfg, ConfigFormat format) {
		this.cfg = cfg;
		this.format = format;
		this.name = name;
		this.iconf = null;
		if (!cfg.exists()) {
			save();
		}
		configs.put(name, this);
	}
	
	public Config(String name, String conf, ConfigFormat format) {
		this.cfg = null;
		this.format = format;
		this.name = name;
		this.iconf = conf;
		configs.put(name, this);
	}
	
	private boolean has(String name) {
		return cm.containsKey(name);
	}
	
	public Object get(String name, RequestPacket request) {
		if (request != null && request.overrideConfig != null && request.overrideConfig.has(name)) return request.overrideConfig.get(name, null);
		return cm.get(name);
	}
	
	public Set<String> keySet(RequestPacket request) {
		if (request != null && request.overrideConfig != null) {
			Set<String> base = new HashSet<String>();
			base.addAll(cm.keySet());
			base.addAll(request.overrideConfig.keySet(null));
			return base;
		}else {
			return cm.keySet();
		}
	}
	
	public void set(String name, String value) {
		cm.put(name, value);
	}
	
	private void format() {
		if (format != null) {
			format.format(cm);
		}
	}
	
	public void load() throws IOException {
		if (cfg != null && !cfg.exists() && cfg.isFile()) {
			format();
			save();
		}
		Scanner in = cfg == null ? new Scanner(iconf) : new Scanner(new FileInputStream(cfg));
		readMap(cm, in);
		in.close();
		format();
	}
	
	private int tabLevel = 0;
	
	private String getWTab() {
		StringBuilder ret = new StringBuilder();
		for (int i = 0; i < tabLevel; i++) {
			ret.append("    ");
		}
		return ret.toString();
	}
	
	private int pl = 0;
	
	private void readMap(HashMap<String, Object> map, Scanner in) {
		while (in.hasNextLine()) {
			String line = in.nextLine().trim();
			if (line.contains("#")) {
				line = line.substring(0, line.indexOf("#")).trim();
			}
			if (line.endsWith("{")) {
				String name = line.substring(0, line.length() - 1);
				boolean base = pl++ == 0;
				if (!base) {
					HashMap<String, Object> nmap = new HashMap<String, Object>();
					map.put(name, nmap);
					readMap(nmap, in);
				}
			}else if (line.equals("}")) {
				pl--;
				break;
			}else if (line.contains("=")) {
				String key = line.substring(0, line.indexOf("="));
				String value = line.substring(key.length() + 1);
				map.put(key, value);
			}
		}
	}
	
	private void writeMap(String name, HashMap<String, Object> map, PrintStream out) {
		out.println(getWTab() + name + "{");
		tabLevel += 1;
		for (String key : map.keySet()) {
			Object o = map.get(key);
			if (o.getClass().equals(map.getClass())) {
				writeMap(key, (HashMap<String, Object>)o, out);
			}else {
				out.println(getWTab() + key + "=" + o);
			}
		}
		tabLevel -= 1;
		out.println(getWTab() + "}");
	}
	
	public void save() {
		if (cfg == null) return;
		format();
		try {
			if (!cfg.getParentFile().exists()) {
				cfg.getParentFile().mkdirs();
			}
			if (!cfg.exists() || !cfg.isFile()) {
				cfg.createNewFile();
			}
			PrintStream out = new PrintStream(new FileOutputStream(cfg));
			writeMap(name, cm, out);
			out.flush();
			out.close();
		}catch (IOException e) {
			Logger.logError(e);
		}
	}
}
