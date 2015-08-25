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
