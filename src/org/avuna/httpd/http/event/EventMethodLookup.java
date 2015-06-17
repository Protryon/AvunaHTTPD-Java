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

package org.avuna.httpd.http.event;

import org.avuna.httpd.event.Event;
import org.avuna.httpd.http.Method;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;

/**
 * You MUST call cancel if you handle the method.
 */
public class EventMethodLookup extends Event {
	
	private final Method method;
	private final RequestPacket request;
	private final ResponsePacket response;
	
	public Method getMethod() {
		return method;
	}
	
	public RequestPacket getRequest() {
		return request;
	}
	
	public ResponsePacket getResponse() {
		return response;
	}
	
	public EventMethodLookup(Method method, RequestPacket request, ResponsePacket response) {
		super(HTTPEventID.METHODLOOKUP);
		this.method = method;
		this.request = request;
		this.response = response;
	}
	
}
