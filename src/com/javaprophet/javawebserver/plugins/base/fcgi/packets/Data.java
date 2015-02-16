package com.javaprophet.javawebserver.plugins.base.fcgi.packets;

import java.io.DataInputStream;
import java.io.IOException;
import com.javaprophet.javawebserver.plugins.base.fcgi.Type;

public class Data extends Stream {
	
	public Data(DataInputStream in) throws IOException {
		super(in);
	}
	
	public Data(int id) {
		super(Type.FCGI_DATA, id);
	}
	
}
