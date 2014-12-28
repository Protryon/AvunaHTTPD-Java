package com.javaprophet.javawebserver;

import java.util.ArrayList;

public class Method {
	
	private static final ArrayList<Method> methods = new ArrayList<Method>();
	
	public static final Method OPTIONS = new Method("OPTIONS");
	public static final Method GET = new Method("GET");
	public static final Method HEAD = new Method("HEAD");
	public static final Method POST = new Method("POST");
	public static final Method PUT = new Method("PUT");
	public static final Method DELETE = new Method("DELETE");
	public static final Method TRACE = new Method("TRACE");
	public static final Method CONNECT = new Method("CONNECT");
	
	public final String name;
	
	private Method(String name) {
		this.name = name;
		methods.add(this);
	}
	
	public static Method get(String name) {
		for (Method m : methods) {
			if (m.name.equals(name)) {
				return m;
			}
		}
		return null;
	}
}
