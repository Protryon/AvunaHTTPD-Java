package org.avuna.httpd.plugins.base.fcgi.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.avuna.httpd.plugins.base.fcgi.Type;

public class Abort extends FCGIPacket {
	
	public Abort(DataInputStream in, int l) throws IOException {
		super(Type.FCGI_ABORT_REQUEST);
		readContent(in, l);
	}
	
	public Abort(int id) {
		super(Type.FCGI_ABORT_REQUEST, id);
	}
	
	@Override
	protected void readContent(DataInputStream in, int l) throws IOException {
		
	}
	
	@Override
	protected void writeContent(DataOutputStream out) throws IOException {
		
	}
	
}
