package org.avuna.httpd.util;

public class ConfigNode {
	private String name, value;
	private ConfigNode[] sub = new ConfigNode[0];
	
	public ConfigNode(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	public ConfigNode(String name) {
		this.name = name;
		this.value = null;
	}
	
	public boolean branching() {
		return sub.length > 0;
	}
	
	public ConfigNode insertNode(ConfigNode subnode) {
		ConfigNode[] nsub = new ConfigNode[sub.length + 1];
		System.arraycopy(sub, 0, nsub, 0, sub.length);
		nsub[nsub.length - 1] = subnode;
		sub = nsub;
		return this;
	}
	
	public boolean containsNode(String name) {
		for (ConfigNode subnode : sub) {
			if (subnode.name.equals(name)) {
				return true;
			}
		}
		return false;
	}
	
	public ConfigNode getNode(String name) {
		for (ConfigNode subnode : sub) {
			if (subnode.name.equals(name)) {
				return subnode;
			}
		}
		return null;
	}
	
	public String getName() {
		return name;
	}
	
	public String getValue() {
		if (branching()) return null;
		return value;
	}
	
}
