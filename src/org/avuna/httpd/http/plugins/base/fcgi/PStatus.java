package org.avuna.httpd.http.plugins.base.fcgi;

public enum PStatus {
	FCGI_REQUEST_COMPLETE(1), FCGI_CANT_MPX_CONN(2), FCGI_OVERLOADED(3), FCGI_UNKNOWN_ROLE(4);
	public final int id;
	
	private PStatus(int id) {
		this.id = id;
	}
	
	public static PStatus fromID(int id) {
		for (PStatus type : values()) {
			if (type.id == id) {
				return type;
			}
		}
		return null;
	}
}
