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

import org.avuna.httpd.util.CLib;

public abstract class GNUTLS {
	
	public static native int globalinit(); // return 0
	
	public static native long loadcert(String ca, String cert, String key); // returns pointer to cert struct, or 0 if failure.
	
	public static native long preaccept(long cert); // returns pointer to session
	
	public static native int postaccept(long cert, long session, int sockfd, Object sniCallback);
	
	public static native byte[] read(long session, int size);
	
	public static native int write(long session, byte[] ba);
	
	public static native int close(long session);
	
	/** This literally does nothing. It's purpose is to call the static initializer early to detect if we have issues before loading. */
	public static void nothing() {
	
	}
	
	static {
		// should be loaded by CLib
		if (CLib.hasGNUTLS() == 1) {
			globalinit();
		}
	}
}
