package com.javaprophet.javawebserver.http;

/**
 * A list of status codes that can be returned
 * Created by Luca on 12/30/2014.
 */
public class StatusCode {
	
	/**
	 * Status 200 OK
	 */
	public static final StatusCode OK = new StatusCode(200, "OK");
	
	/**
	 * Status 206 Partial Content
	 */
	public static final StatusCode PARTIAL_CONTENT = new StatusCode(206, "Partial Content");
	
	/**
	 * Status 401 Unauthorized
	 */
	public static final StatusCode UNAUTHORIZED = new StatusCode(401, "Unauthorized");
	
	/**
	 * Status 403 Forbidden
	 */
	public static final StatusCode FORBIDDEN = new StatusCode(403, "Forbidden");
	
	/**
	 * Status 404 Not Found
	 */
	public static final StatusCode NOT_FOUND = new StatusCode(404, "Not Found");
	
	/**
	 * Status 501 Not Implemented
	 */
	public static final StatusCode NOT_YET_IMPLEMENTED = new StatusCode(501, "Not Implemented");
	
	/**
	 * Status 505 http version not supported
	 */
	public static final StatusCode NEEDS_HTTP_1_1 = new StatusCode(505, "HTTP Version Not Supported");
	
	/**
	 * Status 301 Moved Permanently
	 */
	public static final StatusCode PERM_REDIRECT = new StatusCode(301, "Moved Permanently");
	
	/**
	 * Status 302 Found
	 */
	public static final StatusCode FOUND = new StatusCode(302, "Found");
	
	/**
	 * Status 304 Not Modified
	 */
	public static final StatusCode NOT_MODIFIED = new StatusCode(304, "Not Modified");
	
	/**
	 * Status 500 Internal Server Error
	 */
	public static final StatusCode INTERNAL_SERVER_ERROR = new StatusCode(500, "Internal Server Error");
	
	/**
	 * Status phrase like Internal Server Error
	 */
	private String phrase;
	
	/**
	 * The status code like 200
	 */
	private int status;
	
	/**
	 * Constructor setting status code and phrase
	 * 
	 * @param status the status code such as 200
	 * @param phrase status code such as OK
	 */
	private StatusCode(int status, String phrase) {
		this.phrase = phrase;
		this.status = status;
	}
	
	/**
	 * Get the status phrase
	 * 
	 * @return status phrase
	 */
	public String getPhrase() {
		return phrase;
	}
	
	/**
	 * Get the status code such as 200
	 * 
	 * @return status code
	 */
	public int getStatus() {
		return status;
	}
}