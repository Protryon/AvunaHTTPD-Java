package com.javaprophet.javawebserver.http;

/**
 * Created by Luca on 12/30/2014.
 */
public class StatusCode {
	
	public static final StatusCode OK = new StatusCode(200, "OK");
	public static final StatusCode NOT_FOUND = new StatusCode(404, "File Not Found");
	public static final StatusCode NOT_YET_IMPLEMENTED = new StatusCode(501, "Not Yet Implemented");
	public static final StatusCode NEEDS_HTTP_1_1 = new StatusCode(505, "Needs HTTP/1.1");
	public static final StatusCode PERM_REDIRECT = new StatusCode(301, "Moved Permanently");
	
	private String phrase;
	private int status;
	
	private StatusCode(int status, String phrase) {
		this.phrase = phrase;
		this.status = status;
	}
	
	public String getPhrase() {
		return phrase;
	}
	
	public int getStatus() {
		return status;
	}
}