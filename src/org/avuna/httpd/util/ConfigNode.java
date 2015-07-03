/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.util;

/** General utility for instantiating getting and setting key/values for Avuna *.cfg configuration files.
 * 
 * @author Max */
public class ConfigNode {
	private String name, value, comment = null;
	private ConfigNode[] sub = new ConfigNode[0];
	private ConfigNode parent = null;
	
	/** Instantiate Node with name and value parameters.
	 * 
	 * @param name
	 * @param value */
	public ConfigNode(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	/** Get value of node parent key. By default the parent value is null and must be set using {@link ConfigNode#setParent(ConfigNode)}.
	 * 
	 * @return node parent key value. */
	public ConfigNode getParent() {
		return parent;
	}
	
	/** Sets parent element of node to parent {@link #sub} array.
	 * 
	 * @param parent
	 * @return */
	public ConfigNode setParent(ConfigNode parent) {
		this.parent = parent;
		return this;
	}
	
	/** Instantiate Node with name parameter only.
	 * 
	 * @param name */
	public ConfigNode(String name) {
		this.name = name;
		this.value = null;
	}
	
	/** Sets comment value on node.
	 * 
	 * @param comment
	 * @return */
	public ConfigNode setComment(String comment) {
		this.comment = comment;
		return this;
	}
	
	/** @return true if node is list header. */
	public boolean branching() {
		return sub.length > 0 || value == null;
	}
	
	/** Removes node from {@link #sub} array.
	 * 
	 * @param name
	 * @return new {@link #sub} */
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
	
	/** Instantiate Node with name and call {@link #insertNode(ConfigNode)}.
	 * 
	 * @param name
	 * @return {@link #insertNode(ConfigNode)} */
	public ConfigNode insertNode(String name) {
		return this.insertNode(new ConfigNode(name));
	}
	
	/** Instantiate Node with name, value, and call {@link #insertNode(ConfigNode)}.
	 * 
	 * @param name
	 * @param value
	 * @return {@link #insertNode(ConfigNode)} */
	public ConfigNode insertNode(String name, String value) {
		return this.insertNode(new ConfigNode(name, value));
	}
	
	/** Instantiate Node with name, value, comment and call {@link #insertNode(ConfigNode)}.
	 * 
	 * @param name
	 * @param value
	 * @param comment
	 * @return {@link #insertNode(ConfigNode)} */
	public ConfigNode insertNode(String name, String value, String comment) {
		return this.insertNode(new ConfigNode(name, value).setComment(comment));
	}
	
	/** Used by {@link Config#readMap} to append node to {@link #sub} array.
	 * 
	 * @param subnode
	 * @return updated {@link #sub} array */
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
	
	/** Check if Node exists.
	 * 
	 * @param name
	 * @return true/false */
	public boolean containsNode(String name) {
		for (ConfigNode subnode : sub) {
			if (subnode.name.equals(name)) {
				return true;
			}
		}
		return false;
	}
	
	/** Get Node object.
	 * 
	 * @param name
	 * @return Node object */
	public ConfigNode getNode(String name) {
		for (ConfigNode subnode : sub) {
			if (subnode.name.equals(name)) {
				return subnode;
			}
		}
		return null;
	}
	
	/** Get Node comment.
	 * 
	 * @return Node comment */
	public String getComment() {
		return comment;
	}
	
	/** Get Node name.
	 * 
	 * @return Node name */
	public String getName() {
		return name;
	}
	
	/** @return list of names of {@link #sub} array. */
	public String[] getSubnodes() {
		String[] names = new String[sub.length];
		for (int i = 0; i < sub.length; i++) {
			names[i] = sub[i].name;
		}
		return names;
	}
	
	/** Set value on node if not list header.
	 * 
	 * @param value
	 * @return node with updated value unless list header */
	public ConfigNode setValue(String value) {
		if (branching()) return this;
		this.value = value;
		return this;
	}
	
	/** Get value of node if not list header.
	 * 
	 * @return value if not list header, otherwise null */
	public String getValue() {
		if (branching()) return null;
		return value;
	}
	
	public String toString() {
		return getName() + (branching() ? "" : ("=" + getValue()));
	}
}
