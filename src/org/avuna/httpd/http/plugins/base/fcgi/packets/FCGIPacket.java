package org.avuna.httpd.http.plugins.base.fcgi.packets;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.avuna.httpd.http.plugins.base.fcgi.Type;

public abstract class FCGIPacket {
	public int version = 1; // fcgi version 1 - 8 bits
	public Type type;
	public int id; // 0=management !0=application - 16 bits
	
	protected FCGIPacket(Type type) {
		this.type = type;
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
			packet = new GetValuesResult(in, cl);
			break;
		case FCGI_UNKCOWN_TYPE:
			// TODO: read
			break;
		case FCGI_MAXTYPE:
			// TODO: read
			break;
		}
		in.readFully(new byte[pl]);
		packet.id = id;
		// System.out.println("    {" + type + ", " + id + ", " + pl + "}");
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
	
	public synchronized void write(DataOutputStream out) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		DataOutputStream tout = new DataOutputStream(bout);
		out.write(version);
		out.write(type.id);
		out.writeShort(id);
		writeContent(tout);
		byte[] b = bout.toByteArray();
		// System.out.println("{" + type + ", " + id + ", " + b.length + "}");
		out.writeShort(b.length);
		out.write(0);
		out.write(0);
		out.write(b);
		out.flush();
	}
}
