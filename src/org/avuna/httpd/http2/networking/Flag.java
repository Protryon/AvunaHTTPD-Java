package org.avuna.httpd.http2.networking;

public enum Flag {
	END_STREAM(1), END_HEADERS(4), PADDED(8), PRIORITY(32);
	
	public final int id;
	
	private Flag(int id) {
		this.id = id;
	}
	
	public static Flag getByID(int id) {
		for (Flag type : values()) {
			if (type.id == id) return type;
		}
		return null;
	}
}
