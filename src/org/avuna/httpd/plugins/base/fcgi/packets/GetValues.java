package org.avuna.httpd.plugins.base.fcgi.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.avuna.httpd.plugins.base.fcgi.Type;

public class GetValues extends FCGIPacket {
	
	public GetValues(DataInputStream in, int l) throws IOException {
		super(Type.FCGI_GET_VALUES);
		readContent(in, l);
	}
	
	public GetValues() {
		super(Type.FCGI_GET_VALUES, 0);
	}
	
	@Override
	protected void readContent(DataInputStream in, int l) throws IOException {
		// TODO: NA
	}
	
	@Override
	protected void writeContent(DataOutputStream out) throws IOException {
		String[] names = new String[]{"FCGI_MAX_CONNS", "FCGI_MAX_REQS", "FCGI_MPXS_CONNS"};
		for (String name : names) {
			out.write(name.length());
			out.write(0);
			out.write(name.getBytes());
		}
	}
	
}
