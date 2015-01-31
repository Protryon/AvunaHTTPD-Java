package com.javaprophet.javawebserver.http;

import java.util.ArrayList;
import java.util.HashMap;

public class Headers {
	private HashMap<String, ArrayList<String>> headers = new HashMap<String, ArrayList<String>>();
	
	public Headers() {
		
	}
	
	public Headers clone() {
		Headers h = new Headers();
		h.headers = (HashMap<String, ArrayList<String>>)headers.clone();
		return h;
	}
	
	public void addHeader(String name, String value) {
		if (!headers.containsKey(name)) {
			headers.put(name, new ArrayList<String>());
		}
		headers.get(name).add(value);
	}
	
	public void updateHeader(String name, String value) {
		if (hasHeader(name)) {
			ArrayList<String> hdrs = getHeaders(name);
			hdrs.clear();
			hdrs.add(value);
		}else {
			addHeader(name, value);
		}
	}
	
	public void addHeader(String header) {
		if (header.contains(":")) {
			addHeader(header.substring(0, header.indexOf(":")).trim(), header.substring(header.indexOf(":") + 1).trim());
		}
	}
	
	public ArrayList<String> getHeaders(String name) {
		return headers.get(name);
	}
	
	public String getHeader(String name) {
		for (String key : headers.keySet()) {
			if (key.equals(name) && headers.get(key).size() > 0) {
				return headers.get(key).get(0);
			}
		}
		return null;
	}
	
	public void removeHeaders(String name) {
		String[] keys = headers.keySet().toArray(new String[]{});
		for (int i = 0; i < keys.length; i++) {
			if (keys[i].equals(name)) {
				headers.remove(headers.get(keys[i]));
				i--;
			}
		}
	}
	
	public boolean hasHeader(String name) {
		for (String header : headers.keySet()) {
			if (header.equals(name)) {
				return true;
			}
		}
		return false;
	}
	
	public HashMap<String, ArrayList<String>> getHeaders() {
		return headers;
	}
}
