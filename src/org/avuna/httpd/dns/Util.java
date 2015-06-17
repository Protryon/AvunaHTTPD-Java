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

package org.avuna.httpd.dns;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

/**
 * Created by JavaProphet on 8/14/14 at 1:01 AM.
 */
public class Util {
	public static int getByte(boolean... bit) {
		int br = 0;
		for (int i = 0; i < bit.length; i++) {
			if (bit[bit.length - i - 1]) {
				br += Math.pow(2, i);
			}else {
				br += 0;
			}
		}
		return br;
	}
	
	public static int getNibblet(int pos, int l, byte... data) {
		boolean[] bits = getBits(pos, l, data);
		int value = 0;
		for (int i = 0; i < bits.length; i++) {
			if (bits[i]) value += Math.pow(2, i);
		}
		return value;
	}
	
	public static boolean getBit(int pos, byte... data) {
		int posByte = pos / 8;
		int posBit = pos % 8;
		byte valByte = data[posByte];
		int valInt = valByte >> (8 - (posBit + 1)) & 0x0001;
		return valInt == 1;
	}
	
	public static boolean[] getBits(int pos, int length, byte... data) {
		boolean[] bits = new boolean[length];
		for (int i = 0; i < length; i++) {
			bits[i] = getBit(i + pos, data);
		}
		return bits;
	}
	
	public static boolean[] toNibble(int data, int length) {
		int dc = data;
		boolean[] bits = new boolean[length];
		for (int i = 0; i < length; i++) {
			int pow = (int)Math.pow(2, i);
			if (dc - pow > 0) {
				bits[i] = true;
				dc -= pow;
			}
		}
		return bits;
	}
	
	public static byte[] getUShort(int i) {
		ByteBuffer buf = ByteBuffer.allocate(2);
		buf.order(ByteOrder.BIG_ENDIAN);
		buf.putShort(0, (short)(i));
		return buf.array();
	}
	
	public static int getUShort(byte... b) {
		ByteBuffer buf = ByteBuffer.allocate(2);
		buf.order(ByteOrder.BIG_ENDIAN);
		buf.put(0, b[0]);
		buf.put(1, b[1]);
		return buf.getShort(0);
	}
	
	public static int getUInt(byte... b) {
		ByteBuffer buf = ByteBuffer.allocate(4);
		buf.order(ByteOrder.BIG_ENDIAN);
		buf.put(0, b[0]);
		buf.put(1, b[1]);
		buf.put(2, b[2]);
		buf.put(3, b[3]);
		return buf.getInt(0);
	}
	
	public static byte[] getUInt(int i) {
		ByteBuffer buf = ByteBuffer.allocate(4);
		buf.order(ByteOrder.BIG_ENDIAN);
		buf.putInt(0, i);
		return buf.array();
	}
	
	public static final Random rand = new Random();
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	
	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
	
	public static byte[] ipToByte(String ip) {
		String[] spl1 = ip.split("\\.");
		byte[] spl2 = new byte[spl1.length];
		for (int i = 0; i < spl1.length; i++) {
			spl2[i] = (byte)Integer.parseInt(spl1[i]);
		}
		return spl2;
	}
	
	public static byte[] ip6ToByte(String ip) {
		try {
			InetAddress a = InetAddress.getByName(ip);
			return a.getAddress();
		}catch (Exception e) {
			e.printStackTrace();
		}
		return new byte[16];
	}
	
	public static int getLocationInArray(byte[] major, byte[] minor) {
		int loc = -1;
		int mLen = 0;
		for (int i = 0; i < major.length; i++) {
			if (major[i] == minor[mLen]) {
				if (mLen == 0) {
					loc = i;
				}
				mLen += 1;
			}else {
				loc = -1;
				mLen = 0;
			}
			if (mLen >= minor.length) {
				return loc;
			}
		}
		if (mLen < minor.length) {
			loc = -1;
			mLen = 0;
		}
		return loc;
	}
	
	public static String byteToIp(byte[] ip) {
		String ips = "";
		for (byte i : ip) {
			int i2 = i & 0xFF;
			ips += i2 + ".";
		}
		if (ips.length() > 0) {
			ips = ips.substring(0, ips.length() - 1);
		}
		return ips;
	}
}
