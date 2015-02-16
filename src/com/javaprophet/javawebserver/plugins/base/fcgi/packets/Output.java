package com.javaprophet.javawebserver.plugins.base.fcgi.packets;

import java.io.DataInputStream;
import java.io.IOException;
import com.javaprophet.javawebserver.plugins.base.fcgi.Type;

public class Output extends Stream {
	
	public Output(DataInputStream in, int l) throws IOException {
		readContent(in, l);
	}
	
	public Output(int id) {
		super(Type.FCGI_STDOUT, id);
	}
	
}
