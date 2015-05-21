package org.avuna.httpd.http.plugins.base.fcgi.packets;

import java.io.DataInputStream;
import java.io.IOException;
import org.avuna.httpd.http.plugins.base.fcgi.Type;

public class Error extends Stream {
	
	public Error(DataInputStream in, int l) throws IOException {
		super(Type.FCGI_STDERR);
		readContent(in, l);
	}
	
	public Error(int id) {
		super(Type.FCGI_STDERR, id);
	}
	
}
