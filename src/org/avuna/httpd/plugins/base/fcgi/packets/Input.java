package org.avuna.httpd.plugins.base.fcgi.packets;

import java.io.DataInputStream;
import java.io.IOException;
import org.avuna.httpd.plugins.base.fcgi.Type;

public class Input extends Stream {
	
	public Input(DataInputStream in, int l) throws IOException {
		super(Type.FCGI_STDIN);
		readContent(in, l);
	}
	
	public Input(int id) {
		super(Type.FCGI_STDIN, id);
	}
	
}
