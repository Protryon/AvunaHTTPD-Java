package org.avuna.httpd.plugins.javaloader;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;

public class HTMLBuilder extends PrintWriter {
	private final StringWriter out;
	
	public HTMLBuilder(StringWriter out) {
		super(out);
		this.out = out;
	}
	
	public String toString() {
		return out.toString();
	}
	
	private HashMap<String, Object> vars = null;
	
	public void set(String name, Object value) {
		if (vars == null) vars = new HashMap<String, Object>();
		vars.put(name, value);
	}
	
	public Object get(String name) {
		if (vars == null) return null;
		return vars.get(name);
	}
	
	public boolean containsKey(String name) {
		if (vars == null) return false;
		return vars.containsKey(name);
	}
}
