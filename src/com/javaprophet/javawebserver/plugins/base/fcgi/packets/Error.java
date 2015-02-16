package com.javaprophet.javawebserver.plugins.base.fcgi.packets;

import java.io.DataInputStream;
import java.io.IOException;
import com.javaprophet.javawebserver.plugins.base.fcgi.Type;

public class Error extends Stream {
	
	public Error(DataInputStream in) throws IOException {
		super(in);
	}
	
	public Error(int id) {
		super(Type.FCGI_STDERR, id);
	}
	
}
