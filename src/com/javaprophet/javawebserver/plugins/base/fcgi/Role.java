package com.javaprophet.javawebserver.plugins.base.fcgi;

public enum Role {
	FCGI_RESPONDER(1), FCGI_AUTHORIZER(2), FCGI_FILTER(3);
	public final int id;
	
	private Role(int id) {
		this.id = id;
	}
	
	public static Role fromID(int id) {
		for (Role type : values()) {
			if (type.id == id) {
				return type;
			}
		}
		return null;
	}
}
