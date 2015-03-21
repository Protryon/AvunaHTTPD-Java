package com.javaprophet.javawebserver.http;

import com.javaprophet.javawebserver.http.util.OverrideConfig;

/**
 * Class that provides all the page content
 */
public class Resource {
	
	/**
	 * Page content stored inside data array
	 */
	public byte[] data = new byte[0];
	
	/**
	 * Mime type for the page
	 */
	public String type = "text/html";
	
	/**
	 * Location of the resource
	 */
	public String loc = "/";
	
	/**
	 * Was it a directory?
	 */
	public boolean wasDir = false;
	
	/**
	 * Is it too big?
	 */
	public boolean tooBig = false;
	
	public OverrideConfig effectiveOverride = null;
	
	/**
	 * Clone the resource.
	 * 
	 * @return the cloned resource
	 */
	public Resource clone() {
		Resource res = new Resource(data, type, loc, effectiveOverride);
		res.wasDir = wasDir;
		res.tooBig = tooBig;
		return res;
	}
	
	/**
	 * Constructor setting data and mime type
	 * 
	 * @param data the page data
	 * @param type the mime type
	 */
	public Resource(byte[] data, String type) {
		this.data = data;
		this.type = type;
	}
	
	/**
	 * Constructor setting data, mime type and location
	 * 
	 * @param data the page data
	 * @param type the mime type
	 * @param loc the location
	 */
	public Resource(byte[] data, String type, String loc, OverrideConfig effectiveOverride) {
		this.data = data;
		this.type = type;
		this.loc = loc;
		this.effectiveOverride = effectiveOverride;
	}
}
