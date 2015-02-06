package com.javaprophet.javawebserver.plugins.javaloader;

import java.io.PrintWriter;
import java.io.StringWriter;

public class HTMLBuilder extends PrintWriter {
	private final StringWriter out;
	
	public HTMLBuilder(StringWriter out) {
		super(out);
		this.out = out;
	}
	
	public String toString() {
		return out.toString();
	}
}
