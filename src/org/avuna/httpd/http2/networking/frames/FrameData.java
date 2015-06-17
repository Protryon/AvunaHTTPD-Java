/*	Avuna HTTPD - General Server Applications
    Copyright (C) 2015 Maxwell Bruce

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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
