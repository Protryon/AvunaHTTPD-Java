package org.avuna.httpd.http.event;

public abstract class HTTPEventID {
	public static final int METHODLOOKUP = 7;
	public static final int GENERATERESPONSE = 8;
	public static final int PREPROCESSREQUEST = 9;
	public static final int RESPONSEFINISHED = 10;
	public static final int RESPONSESENT = 11;
	public static final int CLEARCACHE = 12;
}
