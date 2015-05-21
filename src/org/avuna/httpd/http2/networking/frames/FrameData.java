package org.avuna.httpd.http2.networking.frames;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.avuna.httpd.http2.networking.Flag;
import org.avuna.httpd.http2.networking.Frame;
import org.avuna.httpd.http2.networking.FrameType;

public class FrameData extends Frame {
	public byte[] dataPayload;
	public boolean endStream = false;
	
	public FrameData(int streamID, byte[] payload, boolean endStream) {
		super(FrameType.DATA, null, streamID); // flags compiled at writeTime
		this.dataPayload = payload;
		this.endStream = endStream;
	}
	
	@Override
	public void readPayload(DataInputStream in, int len) throws IOException {
		boolean padded = hasFlag(Flag.PADDED);
		int pl = 0;
		if (padded) {
			pl = in.read();
		}
		dataPayload = new byte[len - (padded ? 0 : (1 + pl))];
		in.readFully(dataPayload);
		if (padded) in.readFully(new byte[pl]);
		endStream = hasFlag(Flag.END_STREAM);
	}
	
	@Override
	public void writePayload(DataOutputStream out) throws IOException {
		if (endStream) {
			flags = new Flag[]{Flag.END_STREAM};
		}else {
			flags = null;
		}
		out.write(dataPayload);
	}
}
