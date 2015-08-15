package org.avuna.httpd.util.unio;

public abstract class PacketReceiver {
	
	/** Called when a packet is received.
	 * 
	 * @param sock The socket called on.
	 * @param buf The packet data. */
	public abstract void readPacket(UNIOSocket sock, byte[] buf);
	
	/** Returns a type for the next packet's definition. 0 = delimeter 1 = length
	 * 
	 * @return Delimeter Type. */
	public abstract int nextDelimType(UNIOSocket sock);
	
	/** Called when nextDelimType returns 0.
	 * 
	 * @return The delimiter splitting the next two packets. */
	public byte[] nextDelim(UNIOSocket sock) {
		return null;
	}
	
	/** Called when nextDelimType returns 1.
	 * 
	 * @return The length of the next packet. */
	public int nextLength(UNIOSocket sock) {
		return 0;
	}
	
	public abstract void closed(UNIOSocket sock);
	
	public abstract void fail(Exception e);
}
