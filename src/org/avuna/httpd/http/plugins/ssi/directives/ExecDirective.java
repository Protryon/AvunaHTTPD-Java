/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */
package org.avuna.httpd.http.plugins.ssi.directives;

import org.avuna.httpd.http.Method;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.ssi.Page;
import org.avuna.httpd.http.plugins.ssi.ParsedSSIDirective;
import org.avuna.httpd.http.plugins.ssi.SSIDirective;
import org.avuna.httpd.http.plugins.ssi.SSIEngine;
import org.avuna.httpd.http.plugins.ssi.SSIString;

public class ExecDirective extends SSIDirective {
	
	public ExecDirective(SSIEngine engine) {
		super(engine);
	}
	
	@Override
	public String call(Page page, ParsedSSIDirective dir) {
		if (dir.args.length != 1) return null;
		if (page.data == null || !(page.data instanceof RequestPacket)) return null;
		RequestPacket request = (RequestPacket) page.data;
		if (dir.args[0].startsWith("cgi=")) {
			String f = dir.args[0].substring(4);
			f = new SSIString(f, page, dir).value;
			if (!f.startsWith("/")) f = "/" + f;
			RequestPacket subreq = request.clone();
			subreq.parent = request;
			subreq.target = f;
			subreq.method = Method.GET;
			subreq.body.data = null;
			subreq.headers.removeHeaders("If-None-Matches"); // just in case of collision + why bother ETag?
			subreq.headers.removeHeaders("Accept-Encoding"); // gzip = problem
			ResponsePacket subresp = request.host.getHost().processSubRequests(subreq)[0];
			if (subresp != null && subresp.body != null && subresp.body.data != null) {
				return new String(subresp.body.data);
			}
		}else request.host.logger.logError("Attempted SSI Exec, potential security breach: " + dir.args[0]);
		return null;
	}
	
	@Override
	public String getDirective() {
		return "exec";
	}
	
}
