package org.avuna.httpd.http.util;

public enum Directive {
	forbid("forbid"), redirect("redirect"), index("index"), mime("mime"), cache("cache"), rewrite("rewrite");
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
