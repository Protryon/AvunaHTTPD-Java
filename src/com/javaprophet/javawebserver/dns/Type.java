package com.javaprophet.javawebserver.dns;

public enum Type {
	A("A", 1), NS("NS", 2), MD("MD", 3), MF("MF", 4), CNAME("CNAME", 5), SOA("SOA", 6), MB("MB", 7), MG("MG", 8), MR("MR", 9), NULL("NULL", 10), WKS("WKS", 11), PTR("PTR", 12), HINFO("HINFO", 13), MINFO("MINFO", 14), MX("MX", 15), TXT("TXT", 16), AAAA("AAAA", 28);
	
	public final String name;
	public final int id;
	
	private Type(String name, int id) {
		this.name = name;
		this.id = id;
	}
	
	public static Type getType(int id) {
		for (Type t : values()) {
			if (t.id == id) {
				return t;
			}
		}
		return null;
	}
	
	public static Type getType(String name) {
		for (Type t : values()) {
			if (t.name.equals(name)) {
				return t;
			}
		}
		return null;
	}
	
	public boolean matches(int id) {
		if (id == this.id) return true;
		if (id == 255) return true;
		return false;
	}
}
