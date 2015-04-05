package org.avuna.httpd.http2.networking.frames;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.avuna.httpd.http2.networking.Frame;
import org.avuna.httpd.http2.networking.FrameType;

public class FrameHeaders extends Frame {
	public FrameHeaders(int streamID) {
		super(FrameType.HEADERS, null, streamID); // flags compiled at writeTime
		
	}
	
	@Override
	public void readPayload(DataInputStream in, int len) throws IOException {
		
	}
	
	@Override
	public void writePayload(DataOutputStream out) throws IOException {
		
	}
	
}
