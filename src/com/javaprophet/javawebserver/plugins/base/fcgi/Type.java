package com.javaprophet.javawebserver.plugins.base.fcgi;

public enum Type {
	FCGI_BEGIN_REQUEST(1), FCGI_ABORT_REQUEST(2), FCGI_END_REQUEST(3), FCGI_PARAMS(4), FCGI_STDIN(5), FCGI_STDOUT(6), FCGI_STDERR(7), FCGI_DATA(8), FCGI_GET_VALUES(9), FCGI_GET_VALUES_RESULT(10), FCGI_UNKCOWN_TYPE(11), FCGI_MAXTYPE(-1);
	public final int id;
	
	private Type(int id) {
		this.id = id;
	}
	
	public static Type fromID(int id) {
		for (Type type : values()) {
			if (type.id == id) {
				return type;
			}
		}
		return FCGI_MAXTYPE;
	}
}
