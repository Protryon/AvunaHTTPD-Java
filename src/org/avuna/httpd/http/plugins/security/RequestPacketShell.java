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
package org.avuna.httpd.http.plugins.security;

import org.avuna.httpd.http.Headers;
import org.avuna.httpd.http.Method;
import org.avuna.httpd.http.Resource;
import org.avuna.httpd.http.networking.RequestPacket;

public class RequestPacketShell {
	public final Method method;
	public final String path;
	public final String version;
	public final Headers headers;
	public final Resource resource;
	public final long when = System.nanoTime();
	
	public RequestPacketShell(RequestPacket request) {
		this.method = request.method;
		this.path = request.target;
		this.version = request.httpVersion;
		this.headers = request.headers;
		this.resource = request.body;
	}
}
