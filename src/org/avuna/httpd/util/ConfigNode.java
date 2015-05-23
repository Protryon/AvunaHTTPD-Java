package org.avuna.httpd.util;

/**
 * General utility for instantiating getting and setting key/values for Avuna
 * *.cfg configuration files.
 * 
 * @author Max
 */
public class ConfigNode {
	private String name, value, comment = null;
	private ConfigNode[] sub = new ConfigNode[0];
	private ConfigNode parent = null;
	
	/**
	 * Constructor for node key/value pair
	 * 
	 * @param name
	 * @param value
	 */
	public ConfigNode(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	/**
	 * Get value of node parent key. By default the parent value is null and must be
	 * set using {@link ConfigNode#setParent(ConfigNode)}.
	 * 
	 * @return node parent key value.
	 */
	public ConfigNode getParent() {
		return parent;
	}
	
	/**
	 * Sets value on node parent key.
	 * 
	 * @param parent
	 * @return
	 */
	public ConfigNode setParent(ConfigNode parent) {
		this.parent = parent;
		return this;
	}
	
	/**
	 * Constructor for node.
	 * 
	 * @param name
	 */
	public ConfigNode(String name) {
		this.name = name;
		this.value = null;
	}
	
	/**
	 * Sets comment value on node.
	 * 
	 * @param comment
	 * @return
	 */
	public ConfigNode setComment(String comment) {
		this.comment = comment;
		return this;
	}
	
	/**
	 * @return true if node.sub array length is more than 0 or key has null value.
	 */
	public boolean branching() {
		return sub.length > 0 || value == null;
	}
	
	public ConfigNode removeNode(String name) {
		for (int i = 0; i < sub.length; i++) {
			if (sub[i].name.equals(name)) {
				ConfigNode[] nsub = new ConfigNode[sub.length - 1];
				System.arraycopy(sub, 0, nsub, 0, i);
				System.arraycopy(sub, i + 1, nsub, i, sub.length - (i + 1));
				sub = nsub;
				break;
			}
		}
		return this;
	}
	
	public ConfigNode insertNode(String name) {
		return this.insertNode(new ConfigNode(name));
	}
	
	public ConfigNode insertNode(String name, String value) {
		return this.insertNode(new ConfigNode(name, value));
	}
	
	public ConfigNode insertNode(String name, String value, String comment) {
		return this.insertNode(new ConfigNode(name, value).setComment(comment));
	}
	
	public ConfigNode insertNode(ConfigNode subnode) {
		for (int i = 0; i < sub.length; i++) {
			if (sub[i].name.equals(subnode.name)) {
				sub[i].comment = subnode.comment;
				if (!sub[i].branching()) {
					sub[i].value = subnode.value;
				}else {
					for (int o = 0; o < subnode.sub.length; o++) {
						sub[i].insertNode(subnode.sub[o]);
					}
				}
				return this;
			}
		}
		ConfigNode[] nsub = new ConfigNode[sub.length + 1];
		System.arraycopy(sub, 0, nsub, 0, sub.length);
		nsub[nsub.length - 1] = subnode;
		sub = nsub;
		subnode.setParent(this);
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
	
	public String getComment() {
		return comment;
	}
	
	public String getName() {
		return name;
	}
	
	public String[] getSubnodes() {
		String[] names = new String[sub.length];
		for (int i = 0; i < sub.length; i++) {
			names[i] = sub[i].name;
		}
		return names;
	}
	
	public ConfigNode setValue(String value) {
		if (branching()) return this;
		this.value = value;
		return this;
	}
	
	public String getValue() {
		if (branching()) return null;
		return value;
	}
	
}
