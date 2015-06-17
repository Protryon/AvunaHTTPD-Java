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
		String dom = response.request.headers.hasHeader("Host") ? response.request.headers.getHeader("Host") : "";
		if (dom.contains(":")) dom = dom.substring(0, dom.indexOf(":"));
		this.domain = dom;
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
