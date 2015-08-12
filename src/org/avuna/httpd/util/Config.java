/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Scanner;
import org.avuna.httpd.AvunaHTTPD;

/** General utility for configuration array and file instantiation. Provides methods for reading and writing of formatted configuration files.
 * 
 * @author Max
 * @see ConfigNode
 * @see ConfigFormat abstract for configuration blocks. */
public class Config extends ConfigNode {
	private final File cfg;
	private ConfigFormat format = null;
	private final String iconf;
	public static final HashMap<String, Config> configs = new HashMap<String, Config>();
	
	/** Reads, if exists, or creates configuration array and assigns values.
	 * 
	 * @param name configuration file name and top element of array
	 * @param cfg file name
	 * @param format block information for building array */
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
	
	/** Loads configuration file data from from file or std input if file does not exist.
	 * 
	 * @throws IOException */
	public void load() throws IOException {
		if (cfg != null && !cfg.exists() && cfg.isFile()) {
			format();
			save();
		}
		Scanner in = cfg == null ? new Scanner(iconf) : new Scanner(new FileInputStream(cfg));
		readMap(cfg, this, in);
		in.close();
		format();
	}
	
	private int tabLevel = 0;
	
	/** @return string of 4 characters to create "tab". */
	private String getWTab() {
		StringBuilder ret = new StringBuilder();
		for (int i = 0; i < tabLevel; i++) {
			ret.append("    ");
		}
		return ret.toString();
	}
	
	private int pl = 0;
	private String incl = null;
	
	/** Read configuration file into array.
	 * 
	 * @param map
	 * @param in */
	private void readMap(File f, ConfigNode map, Scanner in) {
		while (in.hasNextLine()) {
			String line = in.nextLine().trim();
			String comment = null;
			if (line.contains("#")) {
				comment = line.substring(line.indexOf("#") + 1).trim();
				line = line.substring(0, line.indexOf("#")).trim();
			}
			if (line.length() == 0) continue;
			if (f != null && line.startsWith("$")) {
				String path = line.substring(1);
				File p;
				if (AvunaHTTPD.windows ? (path.length() < 2 || path.charAt(1) != ':') : !path.startsWith("/")) {
					p = new File(f.getParentFile(), path);
				}else {
					p = new File(path);
				}
				if (!p.canRead() || !p.exists()) {
					System.out.println("Imported config, " + p.getAbsolutePath() + " does not exist, or cannot be read!");
				}
				if (p.isDirectory()) {
					for (File e : p.listFiles()) {
						Config impcfg = new Config(e.getName().substring(0, e.getName().lastIndexOf(".")), e, null);
						impcfg.incl = path;
						impcfg.setComment(comment);
						try {
							impcfg.load();
						}catch (IOException e1) {
							e1.printStackTrace();
							AvunaHTTPD.logger.logError("Failed to read imported config file: " + e.getAbsolutePath());
						}
						map.insertNode(impcfg);
					}
				}else {
					Config impcfg = new Config(p.getName().substring(0, p.getName().lastIndexOf(".")), p, null);
					impcfg.incl = path;
					impcfg.setComment(comment);
					try {
						impcfg.load();
					}catch (IOException e1) {
						e1.printStackTrace();
						AvunaHTTPD.logger.logError("Failed to read imported config file: " + p.getAbsolutePath());
					}
					map.insertNode(impcfg);
				}
			}else if (line.endsWith("{")) {
				String name = line.substring(0, line.length() - 1);
				boolean base = pl++ == 0;
				if (!base) {
					ConfigNode subnode = new ConfigNode(name);
					subnode.setComment(comment);
					map.insertNode(subnode);
					readMap(f, subnode, in);
				}
			}else if (line.equals("}")) {
				pl--;
				break;
			}else if (line.contains("=")) {
				String key = line.substring(0, line.indexOf("="));
				String value = line.substring(key.length() + 1);
				map.insertNode(new ConfigNode(key, value).setComment(comment));
			}else {
				AvunaHTTPD.logger.logError("Invalid config line @ " + (f == null ? "Memory File" : f.getAbsolutePath()));
			}
		}
	}
	
	/** Write configuration array out to json style format.
	 * 
	 * @param map
	 * @param out
	 * @see #save() saves physical file */
	private void writeMap(ConfigNode map, PrintStream out) {
		String c2 = map.getComment();
		out.println(getWTab() + map.getName() + "{" + (c2 != null ? " # " + c2 : ""));
		tabLevel += 1;
		String lincl = null;
		boolean lwc = false;
		for (String key : map.getSubnodes()) {
			ConfigNode o = map.getNode(key);
			if (o.getClass().equals(Config.class)) {
				Config c = (Config) o;
				c.save();
				if (!lwc || !lincl.equals(c.incl)) {
					out.println(getWTab() + "$" + c.incl);
					lincl = c.incl;
				}
				lwc = true;
			}else if (o.branching()) {
				writeMap(o, out);
				lwc = false;
			}else {
				String c = o.getComment();
				out.println(getWTab() + key + "=" + o.getValue() + (c != null ? " # " + c : ""));
				lwc = false;
			}
		}
		tabLevel -= 1;
		out.println(getWTab() + "}");
	}
	
	/** Save physical configuration file, creating parent directories if they don't exist.
	 * 
	 * @throws IOException */
	public void save() {
		if (cfg == null) return;
		format();
		try {
			if (cfg.getParentFile() != null && !cfg.getParentFile().exists()) {
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
			AvunaHTTPD.logger.logError(e);
		}
	}
}
