package org.avuna.httpd.plugins.base.fcgi.packets;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.avuna.httpd.plugins.base.fcgi.Type;

public abstract class FCGIPacket {
	public int version; // fcgi version 1 - 8 bits
	public Type type;
	public int id; // 0=management !0=application - 16 bits
	
	protected FCGIPacket() {
	}
	
	protected FCGIPacket(Type type, int id) {
		this.type = type;
		this.id = id;
		this.version = 1;
	}
	
	public static FCGIPacket read(DataInputStream in) throws IOException {
		int version = in.read();
		Type type = Type.fromID(in.read());
		int id = in.readUnsignedShort();
		int cl = in.readUnsignedShort();
		int pl = in.read();
		in.read(); // reserved
		FCGIPacket packet = null;
		switch (type) {
		case FCGI_BEGIN_REQUEST:
			// TODO: not read
			break;
		case FCGI_ABORT_REQUEST:
			// TODO: not read
			break;
		case FCGI_END_REQUEST:
			packet = new End(in, cl);
			break;
		case FCGI_PARAMS:
			// TODO: not ever read
			break;
		case FCGI_STDIN:
			// TODO: not read
			break;
		case FCGI_STDOUT:
			packet = new Output(in, cl);
			break;
		case FCGI_STDERR:
			packet = new Error(in, cl);
			break;
		case FCGI_DATA:
			// TODO: not read
			break;
		case FCGI_GET_VALUES:
			// TODO: not read
			break;
		case FCGI_GET_VALUES_RESULT:
			// TODO: read
			break;
		case FCGI_UNKCOWN_TYPE:
			// TODO: read
			break;
		case FCGI_MAXTYPE:
			// TODO: read
			break;
		}
		in.readFully(new byte[pl]);
		return packet;
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
