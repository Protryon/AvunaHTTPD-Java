package org.avuna.httpd.http2.hpack;

import java.io.ByteArrayOutputStream;

public class BitOutputStream {
	public BitOutputStream() {
		
	}
	
	private ByteArrayOutputStream bout = new ByteArrayOutputStream();
	private boolean[] buf = new boolean[8];
	private int bi = 0;
	
	public void writeBit(boolean bit) {
		if (bi == 8) {
			int b = 0;
			for (int i = 0; i < 8; i++) {
				if (buf[i]) {
					b += Math.pow(2, i);
					buf[i] = false;
				}
			}
			bout.write(b);
			bi = 0;
		}
		buf[bi++] = bit;
	}
	
	public void writeBinary(String bits) {
		for (char bit : bits.toCharArray()) {
			writeBit(bit == '1');
		}
	}
	
	public byte[] toByteArray() {
		if (bi != 0) {
			int b = 0;
			for (int i = 0; i < 8; i++) {
				if (buf[i]) {
					b += Math.pow(2, i);
					buf[i] = false;
				}
			}
			bout.write(b);
			bi = 0;
		}
		return bout.toByteArray();
	}
}
