package org.avuna.httpd.http.event;

// convenience class
public class HTTPEventID {
	public static final int CONNECTED = 0;
	public static final int DISCONNECTED = 1;
	public static final int POSTINIT = 2;
	public static final int METHODLOOKUP = 3;
	// don't ask why there is no 4.
	public static final int PREEXIT = 5;
	public static final int GENERATERESPONSE = 6;
	public static final int RELOAD = 7;
	public static final int PREPROCESSREQUEST = 8;
	public static final int RESPONSEFINISHED = 9;
	public static final int RESPONSESENT = 10;
	public static final int SETUPFOLDERS = 11;
	public static final int PRECONNECT = 12;
	public static final int CLEARCACHE = 13;
}
