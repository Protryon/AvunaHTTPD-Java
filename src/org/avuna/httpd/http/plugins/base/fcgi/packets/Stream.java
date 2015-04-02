package org.avuna.httpd.http.plugins.base.fcgi.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.avuna.httpd.http.plugins.base.fcgi.Type;

public abstract class Stream extends FCGIPacket {
	public byte[] content = null;
	
	protected Stream(Type type) {
		super(type);
	}
	
	public Stream(Type type, DataInputStream in, int l) throws IOException {
		this(type);
		readContent(in, l);
	}
	
	public Stream(Type type, int id) {
		super(type, id);
	}
	
	@Override
	protected void readContent(DataInputStream in, int l) throws IOException {
		content = new byte[l];
		in.readFully(content);
	}
	
	@Override
	protected void writeContent(DataOutputStream out) throws IOException {
		out.write(content);
	}
	
}
