package org.avuna.httpd.http;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class is used to store key-value properties for an HTTP header.
 */
public class Headers {
	
	/**
	 * This map contains all the headers for the http header.
	 */
	private HashMap<String, ArrayList<String>> headers = new HashMap<String, ArrayList<String>>();
	
	/**
	 * Constructor for the Headers.
	 */
	public Headers() {
		
	}
	
	/**
	 * Clone the current headers into another Headers object.
	 * 
	 * @return a cloned version of the current headers.
	 */
	public Headers clone() {
		Headers h = new Headers();
		h.headers = (HashMap<String, ArrayList<String>>)headers.clone();
		return h;
	}
	
	/**
	 * Add a key-value pair to the headers.
	 * 
	 * @param name the key value
	 * @param value the value to store
	 */
	public void addHeader(String name, String value) {
		if (!headers.containsKey(name)) {
			headers.put(name, new ArrayList<String>());
		}
		headers.get(name).add(value);
	}
	
	/**
	 * Update a header key value.
	 * 
	 * @param name the key to update
	 * @param value the value to set the new key value to
	 */
	public void updateHeader(String name, String value) {
		if (hasHeader(name)) {
			ArrayList<String> hdrs = getHeaders(name);
			hdrs.clear();
			hdrs.add(value);
		}else {
			addHeader(name, value);
		}
	}
	
	/**
	 * Add a header value from a http header such as "content-size: 1337"
	 * 
	 * @param header the http header field.
	 */
	public void addHeader(String header) {
		addHeader(header.substring(0, header.indexOf(":")), header.substring(header.indexOf(":") + 2));
	}
	
	/**
	 * Get the headers for a specific key value.
	 * 
	 * @param name the key name.
	 * @return the headers for the name.
	 */
	public ArrayList<String> getHeaders(String name) {
		return headers.get(name);
	}
	
	/**
	 * Get a header value from the hashmap.
	 * 
	 * @param name the key to get
	 * @return value from the name/key
	 */
	public String getHeader(String name) {
		for (String key : headers.keySet()) {
			if (key.equals(name) && headers.get(key).size() > 0) {
				return headers.get(key).get(0);
			}
		}
		return null;
	}
	
	/**
	 * Remove headers from a key value.
	 * 
	 * @param name the key.
	 */
	public void removeHeaders(String name) {
		String[] keys = headers.keySet().toArray(new String[]{});
		for (int i = 0; i < keys.length; i++) {
			if (keys[i].equals(name)) {
				headers.remove(keys[i]);
			}
		}
	}
	
	/**
	 * Check if the headers contain the certain key.
	 * 
	 * @param name the key to check
	 * @return the state if the headers contain a certain key.
	 */
	public boolean hasHeader(String name) {
		for (String header : headers.keySet()) {
			if (header.equals(name)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Get all the headers.
	 * 
	 * @return the hashmap of all headers.
	 */
	public HashMap<String, ArrayList<String>> getHeaders() {
		return headers;
	}
}
