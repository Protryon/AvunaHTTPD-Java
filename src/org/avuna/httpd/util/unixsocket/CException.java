package org.avuna.httpd.util.unixsocket;

import java.io.IOException;

public class CException extends IOException {
	
	private static final long serialVersionUID = -5344181709439112535L;
	
	public CException(int errorCode, String message) {
		super("C Error: " + errorCode + ", " + message);
	}
	
}
