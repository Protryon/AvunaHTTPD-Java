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
