package org.avuna.httpd.http.plugins.base.fcgi.packets;

import java.io.DataInputStream;
import java.io.IOException;
import org.avuna.httpd.http.plugins.base.fcgi.Type;

public class Output extends Stream {
	
	public Output(DataInputStream in, int l) throws IOException {
		super(Type.FCGI_STDOUT);
		readContent(in, l);
	}
	
	public Output(int id) {
		super(Type.FCGI_STDOUT, id);
	}
	
}
