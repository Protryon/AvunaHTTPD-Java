package com.javaprophet.javawebserver.plugins.base.fcgi.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import com.javaprophet.javawebserver.plugins.base.fcgi.Type;

public class NameValue11 extends FCGIPacket {
	public String name = "";
	public String value = "";
	
	protected NameValue11() {
		
	}
	
	public NameValue11(String name, String value, int id) {
		super(Type.FCGI_PARAMS, id);
		this.name = name;
		this.value = value;
	}
	
	public NameValue11(DataInputStream in, int l) throws IOException {
		readContent(in, l);
	}
	
	@Override
	protected void readContent(DataInputStream in, int l) throws IOException {
		int nl = in.read();
		int vl = in.read();
		name = readUTF(in, nl);
		value = readUTF(in, vl);
	}
	
	@Override
	protected void writeContent(DataOutputStream out) throws IOException {
		out.write(name.length());
		out.write(value.length());
		out.write(name.getBytes());
		out.write(value.getBytes());
	}
	
}
