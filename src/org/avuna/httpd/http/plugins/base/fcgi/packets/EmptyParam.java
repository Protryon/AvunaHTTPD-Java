package org.avuna.httpd.http.plugins.base.fcgi.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.avuna.httpd.http.plugins.base.fcgi.Type;

public class EmptyParam extends FCGIPacket {
	
	public EmptyParam(int id) {
		super(Type.FCGI_PARAMS, id);
	}
	
	public EmptyParam(DataInputStream in, int l) throws IOException {
		super(Type.FCGI_PARAMS);
		readContent(in, l);
	}
	
	@Override
	protected void readContent(DataInputStream in, int l) throws IOException {
	}
	
	@Override
	protected void writeContent(DataOutputStream out) throws IOException {
	}
	
}
