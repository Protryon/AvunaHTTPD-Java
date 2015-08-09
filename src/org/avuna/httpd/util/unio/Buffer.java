package org.avuna.httpd.util.unio;

public class Buffer {
	protected byte[] buf;
	private int read = 0;
	private int length = 0;
	private PacketReceiver callback;
	private UNIOSocket socket;
	private int pt = -1;
	private int pl = -1;
	private byte[] pd = null;
	
	/** Creates a new NIO Buffer.
	 * 
	 * @param size Initial size of the buffer. */
	protected Buffer(int size, PacketReceiver callback, UNIOSocket socket) {
		buf = new byte[size];
		this.callback = callback;
		this.socket = socket;
		pt = callback.nextDelimType(socket);
		if (pt == 0) {
			pd = callback.nextDelim(socket);
		}else if (pt == 1) {
			pl = callback.nextLength(socket);
		}
	}
	
	public void append(byte[] buf) {
		append(buf, 0, buf.length);
	}
	
	private void ensureCapacity(int size) {
		synchronized (buf) {
			if (buf.length - read < size) {
				byte[] nb = new byte[size];
				System.arraycopy(buf, read, nb, 0, Math.min(length, size));
				this.buf = nb;
				this.read = 0;
			}
		}
	}
	
	public void append(byte[] buf, int offset, int length) {
		ensureCapacity(this.length + length);
		synchronized (buf) {
			System.arraycopy(buf, offset, this.buf, this.read + this.length, length);
			if (pt == 0) {
				int ml = 0;
				for (int i = offset; i < offset + length; i++) {
					if (buf[i] == pd[ml]) {
						ml++;
						if (ml == pd.length) {
							byte[] packet = new byte[i - offset + this.length];
							System.arraycopy(this.buf, this.read, packet, 0, packet.length);
							this.read += packet.length;
							callback.readPacket(socket, packet);
							pt = callback.nextDelimType(socket);
							if (pt == 0) {
								pd = callback.nextDelim(socket);
							}else if (pt == 1) {
								pl = callback.nextLength(socket);
							}
							break;
						}
					}else if (ml > 0) {
						ml = 0;
					}
				}
			}else if (pt == 1) {
				if (this.length + length > pl) {
					byte[] packet = new byte[pl];
					System.arraycopy(this.buf, this.read, packet, 0, packet.length);
					this.read += packet.length;
					callback.readPacket(socket, packet);
					pt = callback.nextDelimType(socket);
					if (pt == 0) {
						pd = callback.nextDelim(socket);
					}else if (pt == 1) {
						pl = callback.nextLength(socket);
					}
				}
			}
			this.length += length;
		}
	}
	/*
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
	}*/
}
