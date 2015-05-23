package org.avuna.httpd.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Scanner;

public class Config extends ConfigNode {
	private final File cfg;
	private ConfigFormat format = null;
	private final String iconf;
	public static final HashMap<String, Config> configs = new HashMap<String, Config>();
	
	public Config(String name, File cfg, ConfigFormat format) {
		super(name);
		this.cfg = cfg;
		this.format = format;
		this.iconf = null;
		if (!cfg.exists()) {
			save();
		}
		configs.put(name, this);
	}
	
	public Config(String name, String conf, ConfigFormat format) {
		super(name);
		this.cfg = null;
		this.format = format;
		this.iconf = conf;
		configs.put(name, this);
	}
	
	public File getFile() {
		return cfg;
	}
	
	private void format() {
		if (format != null) {
			format.format(this);
		}
	}
	
	public void load() throws IOException {
		if (cfg != null && !cfg.exists() && cfg.isFile()) {
			format();
			save();
		}
		Scanner in = cfg == null ? new Scanner(iconf) : new Scanner(new FileInputStream(cfg));
		readMap(this, in);
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
	
	private void readMap(ConfigNode map, Scanner in) {
		while (in.hasNextLine()) {
			String line = in.nextLine().trim();
			String comment = null;
			if (line.contains("#")) {
				comment = line.substring(line.indexOf("#") + 1).trim();
				line = line.substring(0, line.indexOf("#")).trim();
			}
			if (line.endsWith("{")) {
				String name = line.substring(0, line.length() - 1);
				boolean base = pl++ == 0;
				if (!base) {
					ConfigNode subnode = new ConfigNode(name);
					map.insertNode(subnode).setComment(comment);
					readMap(subnode, in);
				}
			}else if (line.equals("}")) {
				pl--;
				break;
			}else if (line.contains("=")) {
				String key = line.substring(0, line.indexOf("="));
				String value = line.substring(key.length() + 1);
				map.insertNode(new ConfigNode(key, value)).setComment(comment);
			}
		}
	}
	
	private void writeMap(ConfigNode map, PrintStream out) {
		String c2 = map.getComment();
		out.println(getWTab() + map.getName() + "{" + (c2 != null ? " # " + c2 : ""));
		tabLevel += 1;
		for (String key : map.getSubnodes()) {
			ConfigNode o = map.getNode(key);
			if (o.branching()) {
				writeMap(o, out);
			}else {
				String c = o.getComment();
				out.println(getWTab() + key + "=" + o.getValue() + (c != null ? " # " + c : ""));
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
			writeMap(this, out);
			out.flush();
			out.close();
		}catch (IOException e) {
			Logger.logError(e);
		}
	}
}
