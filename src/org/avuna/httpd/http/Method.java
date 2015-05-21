package org.avuna.httpd.http;

import java.util.ArrayList;

/**
 * Http Methods.
 */
public class Method {
	
	/**
	 * A list of methods.
	 */
	private static final ArrayList<Method> methods = new ArrayList<Method>();
	
	/**
	 * Http Method for OPTIONS
	 */
	public static final Method OPTIONS = new Method("OPTIONS");
	
	/**
	 * Http Method for GET
	 */
	public static final Method GET = new Method("GET");
	
	/**
	 * Http Method for HEAD
	 */
	public static final Method HEAD = new Method("HEAD");
	/**
	 * Http Method for POST
	 */
	public static final Method POST = new Method("POST");
	
	/**
	 * Http Method for PUT
	 */
	public static final Method PUT = new Method("PUT");
	
	/**
	 * Http Method for DELETE
	 */
	public static final Method DELETE = new Method("DELETE");
	
	/**
	 * Http Method for TRACE
	 */
	public static final Method TRACE = new Method("TRACE");
	
	/**
	 * Http Method for CONNECT
	 */
	public static final Method CONNECT = new Method("CONNECT");
	
	/**
	 * Http Method for PRI
	 * NOTE* This is for HTTP/2 UPGRADING ONLY!!!
	 */
	public static final Method PRI = new Method("PRI");
	
	/**
	 * The method's name such as POST
	 */
	public final String name;
	
	/**
	 * Constructor setting the method name
	 * 
	 * @param name the method name such as POST
	 */
	private Method(String name) {
		this.name = name;
		methods.add(this);
	}
	
	/**
	 * Get a method by its name
	 * 
	 * @param name the method name
	 * @return the method corresponding to the name and null if not found.
	 */
	public static Method get(String name) {
		for (Method m : methods) {
			if (m.name.equals(name)) {
				return m;
			}
		}
		
		return null;
	}
}
