package org.avuna.httpd.http2.hpack;

public class BitInputStream {
	private final byte[] buf;
	
	public BitInputStream(byte[] buf) {
		this.buf = buf;
	}
	
	private int opos = 0;
	private int bpos = 0;
	private static final byte[] bitmask = new byte[]{1, 2, 4, 8, 16, 32, 64, (byte)128};
	
	public boolean readBit() {
		if (opos == buf.length) return false;
		byte b = buf[opos];
		boolean bit = (bitmask[bpos] & b) == bitmask[bpos++];
		if (bpos == 8) {
			bpos = 0;
			opos++;
		}
		return bit;
	}
	
	public boolean available() {
		return opos < buf.length;
	}
}
