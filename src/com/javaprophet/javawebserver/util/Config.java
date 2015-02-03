package com.javaprophet.javawebserver.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;

public class Config {
	private final File cfg;
	private ConfigFormat format = null;
	private HashMap<String, Object> cm = new HashMap<String, Object>();
	
	public Config(File cfg, ConfigFormat format) {
		this.cfg = cfg;
		this.format = format;
		if (!cfg.exists()) {
			save();
		}
	}
	
	public Object get(String name) {
		return cm.get(name);
	}
	
	public Set<String> keySet() {
		return cm.keySet();
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
		if (!cfg.exists() && cfg.isFile()) {
			format();
			save();
		}
		Scanner in = new Scanner(new FileInputStream(cfg));
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
		format();
		try {
			if (!cfg.getParentFile().exists()) {
				cfg.getParentFile().mkdirs();
			}
			if (!cfg.exists() || !cfg.isFile()) {
				cfg.createNewFile();
			}
			PrintStream out = new PrintStream(new FileOutputStream(cfg));
			writeMap(cfg.getName().substring(0, cfg.getName().indexOf(".")), cm, out);
			out.flush();
			out.close();
		}catch (IOException e) {
			Logger.logError(e);
		}
	}
}
