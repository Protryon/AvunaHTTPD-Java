package org.avuna.httpd.http.plugins.base.fcgi.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.avuna.httpd.http.plugins.base.fcgi.Type;

public class Params extends FCGIPacket {
	public byte[] data;
	
	protected Params() {
		super(Type.FCGI_PARAMS);
	}
	
	public Params(byte[] data, int id) {
		super(Type.FCGI_PARAMS, id);
		this.data = data;
	}
	
	public Params(DataInputStream in, int l) throws IOException {
		super(Type.FCGI_PARAMS);
		readContent(in, l);
	}
	
	@Override
	protected void readContent(DataInputStream in, int l) throws IOException {
		// TODO: unnecessary, but for a universal api, yes
	}
	
	@Override
	protected void writeContent(DataOutputStream out) throws IOException {
		// System.out.println(AvunaHTTPD.fileManager.bytesToHex(data));
		out.write(data);
	}
	
}
