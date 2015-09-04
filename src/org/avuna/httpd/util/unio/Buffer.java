/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */
package org.avuna.httpd.util.unio;

import java.io.InputStream;
import java.lang.Thread.State;

public class Buffer extends InputStream {
	protected byte[] buf;
	private int read = 0;
	private int length = 0;
	private PacketReceiver callback;
	private UNIOSocket socket;
	private int pt = -1;
	private int pl = -1;
	private byte[] pd = null;
	private Thread flushInterrupt = null;
	
	public UNIOSocket getSocket() {
		return socket;
	}
	
	public void setFlushInterruptThread(Thread t) {
		flushInterrupt = t;
	}
	
	/** Creates a new NIO Buffer.
	 * 
	 * @param size Initial size of the buffer. */
	protected Buffer(int size, PacketReceiver callback, UNIOSocket socket) {
		buf = new byte[size];
		this.callback = callback;
		this.socket = socket;
		if (callback != null) {
			try {
				pt = callback.nextDelimType(socket);
				if (pt == 0) {
					pd = callback.nextDelim(socket);
				}else if (pt == 1) {
					pl = callback.nextLength(socket);
				}
			}catch (Exception e) {
				callback.fail(e);
			}
		}
	}
	
	public void append(byte[] buf) {
		append(buf, 0, buf.length);
	}
	
	public void append(byte[] buf, int offset, int length) {
		if (length == 0 || this.length < 0) return;
		synchronized (this.buf) {
			int size = this.length + length + this.read;
			if (this.buf.length - read < size) {
				byte[] nb = new byte[size + 1024];
				System.arraycopy(this.buf, read, nb, 0, Math.min(this.length, size + 1024));
				this.buf = nb;
				this.read = 0;
			}
			System.arraycopy(buf, offset, this.buf, this.read + this.length, length);
			if (callback != null) {
				int ml = 0;
				for (int i = Math.max(0, this.read + this.length - pd.length + 1); i < this.read + this.length + length; i++) {
					if (pt == 0) {
						if (this.buf[i] == pd[ml]) {
							ml++;
							if (ml == pd.length) {
								byte[] packet = new byte[i + 1 - this.read];
								System.arraycopy(this.buf, this.read, packet, 0, packet.length);
								this.read += packet.length;
								this.length -= packet.length;
								try {
									callback.readPacket(socket, packet);
									pt = callback.nextDelimType(socket);
									if (pt == 0) {
										pd = callback.nextDelim(socket);
									}else if (pt == 1) {
										pl = callback.nextLength(socket);
									}
								}catch (Exception e) {
									callback.fail(e);
								}
								ml = 0;
							}
						}else if (ml > 0) {
							ml = 0;
						}
					}else if (pt == 1) {
						if (this.length + length >= pl) {
							byte[] packet = new byte[pl];
							System.arraycopy(this.buf, this.read, packet, 0, packet.length);
							this.read += packet.length;
							this.length -= packet.length;
							try {
								callback.readPacket(socket, packet);
								pt = callback.nextDelimType(socket);
								if (pt == 0) {
									pd = callback.nextDelim(socket);
								}else if (pt == 1) {
									pl = callback.nextLength(socket);
								}
							}catch (Exception e) {
								callback.fail(e);
							}
						}else {
							break;
						}
					}
				}
			}
			this.length += length;
		}
		if (flushInterrupt != null && flushInterrupt.getState() == State.TIMED_WAITING) {
			synchronized (flushInterrupt) {
				flushInterrupt.notify();
			}
		}
	}
	
	/** Only to be used for prepending data that was not successfully written, and was just pulled out. No other use is safe. */
	protected void unsafe_prepend(byte[] buf, int offset, int length) {
		synchronized (this.buf) {
			System.arraycopy(buf, offset, this.buf, this.read - length, length);
			this.read -= length;
			this.length += length;
		}
		if (flushInterrupt != null && flushInterrupt.getState() == State.TIMED_WAITING) {
			synchronized (flushInterrupt) {
				flushInterrupt.notify();
			}
		}
	}
	
	@Override
	public int read() {
		synchronized (buf) {
			if (read > length + buf.length) {
				return -1;
			}
			return buf[read++];
		}
	}
	
	public int read(byte[] buf) {
		return read(buf, 0, buf.length);
	}
	
	public int read(byte[] buf, int offset, int length) {
		synchronized (this.buf) {
			int rs = Math.min(length, buf.length - offset);
			rs = Math.min(rs, this.length);
			if (rs < 1) return 0;
			System.arraycopy(this.buf, read, buf, offset, rs);
			read += rs;
			this.length -= rs;
			return rs;
		}
	}
	
	public int available() {
		synchronized (buf) {
			return length;
		}
	}
	
	public long skip(long n) {
		synchronized (buf) {
			if (this.length < n) {
				throw new ArrayIndexOutOfBoundsException("n must be <= length");
			}
			int ts = (int) Math.min(length, n);
			this.read += ts;
			this.length -= ts;
			return ts;
		}
	}
}
