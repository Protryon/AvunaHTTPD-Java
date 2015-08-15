package org.avuna.httpd.util.unio;

import java.io.InputStream;
import java.lang.Thread.State;
import org.avuna.httpd.AvunaHTTPD;

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
		if (length == 0) return;
		synchronized (buf) {
			int size = this.length + length + this.read;
			if (this.buf.length - read < size) {
				byte[] nb = new byte[size + 1024];
				System.arraycopy(this.buf, read, nb, 0, Math.min(this.length, size + 1024));
				this.buf = nb;
				this.read = 0;
			}
			System.out.println(AvunaHTTPD.fileManager.bytesToHex(buf));
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
		synchronized (buf) {
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
		synchronized (buf) {
			int rs = Math.min(length, buf.length - offset);
			rs = Math.min(rs, this.length);
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
			int ts = (int) Math.min(length, n);
			this.read += ts;
			this.length -= ts;
			return ts;
		}
	}
}
