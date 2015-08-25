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
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.avuna.httpd.util.unio;

import java.io.IOException;
import java.io.OutputStream;

public class BufferOutputStream extends OutputStream {
	private final Buffer buf;
	
	public BufferOutputStream(Buffer buf) {
		this.buf = buf;
	}
	
	@Override
	public void write(int b) throws IOException {
		buf.append(new byte[] { (byte) b }, 0, 1);
	}
	
	@Override
	public void write(byte[] b) throws IOException {
		buf.append(b, 0, b.length);
	}
	
	@Override
	public void write(byte[] b, int offset, int length) throws IOException {
		buf.append(b, offset, length);
	}
	
}
