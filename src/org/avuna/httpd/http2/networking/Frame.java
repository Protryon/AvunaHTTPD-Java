package org.avuna.httpd.http2.networking;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Frame {
	public FrameType type;
	public int flags;
	public int streamID;
	public byte[] payload;
	
	public Frame(FrameType type, int flags, int streamID, byte[] payload) {
		this.type = type;
		this.flags = flags;
		this.streamID = streamID;
		this.payload = payload;
	}
	
	public Frame(FrameType type, int flags, int streamID) {
		this(type, flags, streamID, new byte[0]);
	}
	
	public Frame() {
		
	}
	
	public void read(DataInputStream in) throws IOException {
		byte[] lenb = new byte[3];
		in.readFully(lenb);
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.put(0, (byte)0x0);
		bb.position(1);
		bb.put(lenb);
		payload = new byte[bb.getInt(0)];
		type = FrameType.getByID(in.read());
		flags = in.read();
		streamID = in.readInt();
		in.readFully(payload);
	}
}
