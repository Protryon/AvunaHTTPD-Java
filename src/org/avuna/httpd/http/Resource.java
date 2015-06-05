package org.avuna.httpd.http;

import org.avuna.httpd.http.util.OverrideConfig;

public class Resource {
	
	public byte[] data = new byte[0];
	
	public String type = "text/html";
	
	public String loc = "/";
	
	public boolean wasDir = false;
	
	public boolean tooBig = false;
	
	public OverrideConfig effectiveOverride = null;
	public String oabs = "";// rewrite stuff
	
	public Resource clone() {
		Resource res = new Resource(data, type, loc, effectiveOverride, oabs);
		res.wasDir = wasDir;
		res.tooBig = tooBig;
		return res;
	}
	
	public Resource(byte[] data, String type) {
		this.data = data;
		this.type = type;
	}
	
	public Resource(byte[] data, String type, String loc, OverrideConfig effectiveOverride, String oabs) {
		this.data = data;
		this.type = type;
		this.loc = loc;
		this.effectiveOverride = effectiveOverride;
		this.oabs = oabs;
	}
}
