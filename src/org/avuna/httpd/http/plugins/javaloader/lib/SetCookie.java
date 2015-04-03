package org.avuna.httpd.http.plugins.javaloader.lib;

import org.avuna.httpd.http.networking.ResponsePacket;

public class SetCookie {
	private final ResponsePacket response;
	private final String domain;
	
	public SetCookie(ResponsePacket response, String domain) {
		this.response = response;
		this.domain = domain;
	}
	
	public SetCookie(ResponsePacket response) {
		this.response = response;
		this.domain = response.request.headers.hasHeader("Host") ? response.request.headers.getHeader("Host") : "";
	}
	
	public void setCookie(String name, String value) {
		response.headers.addHeader("Set-Cookie", name + "=" + value + "; Path=/; Domain=" + domain);
	}
	
	public void setCookie(String name, String value, int maxage) {
		response.headers.addHeader("Set-Cookie", name + "=" + value + "; Max-Age=" + maxage + "; Path=/; Domain=" + domain);
	}
	
	public void setCookie(String name, String value, int maxage, String path) {
		response.headers.addHeader("Set-Cookie", name + "=" + value + "; Max-Age=" + maxage + "; Path=" + path + "; Domain=" + domain);
	}
	
	public void setCookie(String name, String value, int maxage, String path, String domain) {
		response.headers.addHeader("Set-Cookie", name + "=" + value + "; Max-Age=" + maxage + "; Path=" + path + "; Domain=" + domain);
	}
	
	public void setCookie(String name, String value, int maxage, String path, String domain, boolean secure, boolean httponly) {
		response.headers.addHeader("Set-Cookie", name + "=" + value + "; Max-Age=" + maxage + "; Path=" + path + "; Domain=" + domain + (secure ? "; Secure" : "") + (httponly ? "; HttpOnly" : ""));
	}
}