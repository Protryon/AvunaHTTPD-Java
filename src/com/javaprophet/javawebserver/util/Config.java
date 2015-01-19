package com.javaprophet.javawebserver.util;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Config {
	private final File cfg;
	private JSONObject json = new JSONObject();
	private ConfigFormat format = null;
	
	public Config(File cfg, ConfigFormat format) {
		this.cfg = cfg;
		this.format = format;
		if (!cfg.exists()) {
			save();
		}
	}
	
	public Object get(String name) {
		return json.get(name);
	}
	
	public Set keySet() {
		return json.keySet();
	}
	
	public void set(String name, String value) {
		json.put(name, value);
	}
	
	private void format() {
		if (format != null) {
			format.format(json);
		}
	}
	
	public void load() throws IOException, ParseException {
		if (!cfg.exists() && cfg.isFile()) {
			format();
			save();
		}
		DataInputStream in = new DataInputStream(new FileInputStream(cfg));
		byte[] buf = new byte[4096];
		int i = 1;
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		while (i > 0) {
			i = in.read(buf);
			if (i > 0) {
				bout.write(buf, 0, i);
			}
		}
		JSONParser parse = new JSONParser();
		json = (JSONObject)parse.parse(new String(bout.toByteArray()));
		format();
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
			DataOutputStream out = new DataOutputStream(new FileOutputStream(cfg));
			out.write(json.toJSONString().getBytes());
			out.flush();
			out.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
}
