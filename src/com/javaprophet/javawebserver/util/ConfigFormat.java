package com.javaprophet.javawebserver.util;

import org.json.simple.JSONObject;

public abstract class ConfigFormat {
	public ConfigFormat() {
		
	}
	
	public abstract void format(JSONObject json);
}
