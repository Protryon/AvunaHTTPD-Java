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

package org.avuna.httpd.http2.networking;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class Frame {
	public FrameType type;
	public Flag[] flags;
	public int streamID;
	
	public Frame(FrameType type, Flag[] flags, int streamID) {
		this.type = type;
		this.flags = flags;
		this.streamID = streamID;
	}
	
	public Frame() {
		
	}
	
	public final void read(DataInputStream in) throws IOException {
		byte[] lenb = new byte[3];
		in.readFully(lenb);
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.put(0, (byte)0x0);
		bb.position(1);
		bb.put(lenb);
		int len = bb.getInt(0);
		type = FrameType.getByID(in.read());
		int flag = in.read();
		if (flag == 0) {
			flags = null;
		}else {
			Flag[] bf = new Flag[8];
			int bfi = 0;
			for (int i = 7; i > 0; i--) {
				int f = (int)Math.pow(2, i);
				if (flag >= f) {
					flag -= f;
				}else {
					continue;
				}
				Flag fc = Flag.getByID(f);
				if (fc != null) {
					bf[bfi++] = fc;
				}
				if (flag == 0) break;
			}
			Flag[] tfb = new Flag[bfi];
			System.arraycopy(bf, 0, tfb, 0, bfi);
			bf = tfb;
			tfb = null;
		}
		streamID = in.readInt();
		readPayload(in, len);
	}
	
	public final boolean hasFlag(Flag f) {
		for (Flag ff : flags) {
			if (ff.id == f.id) {
				return true;
			}
		}
		return false;
	}
	
	public final void write(DataOutputStream out) throws IOException {
		byte[] lenb = new byte[3];
		ByteBuffer bb = ByteBuffer.allocate(4);
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		writePayload(new DataOutputStream(buf));
		bb.putInt(0, buf.size());
		bb.get(lenb, 1, 3);
		out.write(lenb);
		out.write(type.id);
		int ff = 0;
		for (Flag f : flags) {
			ff += f.id;
		}
		out.write(ff);
		out.writeInt(streamID);
		out.write(buf.toByteArray());
	}
	
	public abstract void readPayload(DataInputStream in, int len) throws IOException;
	
	public abstract void writePayload(DataOutputStream out) throws IOException;
}
