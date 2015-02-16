package com.javaprophet.javawebserver.plugins.base.fcgi.packets;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import com.javaprophet.javawebserver.plugins.base.fcgi.Type;

public abstract class FCGIPacket {
	public final int version; // fcgi version 1 - 8 bits
	public final Type type;
	public final int id; // 0=management !0=application - 16 bits
	
	public FCGIPacket(Type type, int id) {
		this.type = type;
		this.id = id;
		this.version = 1;
	}
	
	public FCGIPacket(DataInputStream in) throws IOException {
		version = in.read();
		type = Type.fromID(in.read());
		id = in.readUnsignedShort();
		int cl = in.readUnsignedShort();
		int pl = in.read();
		in.read(); // reserved
		readContent(in, cl);
		in.readFully(new byte[pl]);
	}
	
	protected final static String readUTF(DataInputStream in, int l) throws IOException {
		char[] ca = new char[l];
		for (int i = 0; i < l; i++) {
			ca[i] = in.readChar();
		}
		return new String(ca);
	}
	
	protected abstract void readContent(DataInputStream in, int l) throws IOException;
	
	protected abstract void writeContent(DataOutputStream out) throws IOException;
	
	private final ByteArrayOutputStream bout = new ByteArrayOutputStream();
	private final DataOutputStream tout = new DataOutputStream(bout);
	
	public synchronized void write(DataOutputStream out) throws IOException {
		out.write(version);
		out.write(type.id);
		out.writeShort(id);
		bout.reset();
		writeContent(tout);
		out.writeShort(bout.size());
		out.write(0);
		out.write(0);
		out.write(bout.toByteArray());
		out.flush();
	}
}
