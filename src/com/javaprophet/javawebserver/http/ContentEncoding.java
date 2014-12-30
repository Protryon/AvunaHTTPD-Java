package com.javaprophet.javawebserver.http;

import java.util.ArrayList;

/**
 * This class contains some encoding methods that send information to the user.
 */
public class ContentEncoding {
	private static final ArrayList<ContentEncoding> ces = new ArrayList<ContentEncoding>();

	/**
	 * Compress encoding.
	 */
	public static final ContentEncoding compress = new ContentEncoding("compress");
	/**
	 * Deflate encoding.
	 */
	public static final ContentEncoding deflate = new ContentEncoding("deflate");
	/**
	 * Exi Encoding
	 */
	public static final ContentEncoding exi = new ContentEncoding("exi");
	/**
	 * GZip encoding
	 */
	public static final ContentEncoding gzip = new ContentEncoding("gzip");
	/**
	 * Identity thing
	 */
	public static final ContentEncoding identity = new ContentEncoding("identity");
	public static final ContentEncoding pack200gzip = new ContentEncoding("pack200-gzip");
	public static final ContentEncoding xcompress = new ContentEncoding("x-compress");
	public static final ContentEncoding xgzip = new ContentEncoding("x-gzip");
	public String name = "";
	
	private ContentEncoding(String name) {
		this.name = name;
		ces.add(this);
	}

	/**
	 * Get the name of the encoding.
	 * @return
	 */
	public String toString() {
		if (this == identity) {
			return "";
		}else {
			return name;
		}
	}

	/**
	 * Get encoding class by name.
	 * @param name
	 * @return
	 */
	public static ContentEncoding get(String name) {
		String n = name;
		if (n.contains(";q=")) {
			n = n.substring(0, n.indexOf(";q="));
		}
		if (n.equals("")) return identity;
		for (ContentEncoding ce : ces) {
			if (ce.name.equals(n)) {
				return ce;
			}
		}
		return null;
	}
}
