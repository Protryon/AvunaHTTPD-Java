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
