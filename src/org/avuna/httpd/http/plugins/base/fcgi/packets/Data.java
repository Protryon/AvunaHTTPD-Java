package org.avuna.httpd.http.plugins.base.fcgi.packets;

import java.io.DataInputStream;
import java.io.IOException;
import org.avuna.httpd.http.plugins.base.fcgi.Type;

public class Data extends Stream {
	
	public Data(DataInputStream in, int l) throws IOException {
		super(Type.FCGI_DATA);
		readContent(in, l);
	}
	
	public Data(int id) {
		super(Type.FCGI_DATA, id);
	}
	
}
