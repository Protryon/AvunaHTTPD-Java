package com.javaprophet.javawebserver.util;

public enum Directive {
	forbid("forbid"), redirect("redirect"), index("index");
	public final String name;
	
	Directive(String name) {
		this.name = name;
	}
	
	public static Directive getDirective(String name) {
		for (Directive d : Directive.values()) {
			if (d.name.equals(name)) return d;
		}
		return null;
	}
}
