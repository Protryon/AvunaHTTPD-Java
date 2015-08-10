package org.avuna.httpd.util.unio;

import org.avuna.httpd.util.CLib;

public abstract class GNUTLS {
	
	public static native int global_init(); // return 0
	
	public static native long load_cert(String ca, String crl, String cert, String key); // returns pointer to cert struct, or 0 if failure.
	
	public static native long preaccept(long cert); // returns pointer to session
	
	public static native int postaccept(long cert, long session, int sockfd);
	
	public static native byte[] read(long session, int size);
	
	public static native int write(long session, byte[] ba);
	
	public static native int close(long session);
	
	static {
		// should be loaded by CLib
		CLib.nothing();
	}
}
