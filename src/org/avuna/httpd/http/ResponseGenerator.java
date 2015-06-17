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

package org.avuna.httpd.http;

import java.text.SimpleDateFormat;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.http.event.EventMethodLookup;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.util.Logger;

/**
 * Creates a http response coming from the server.
 */
public class ResponseGenerator {
	
	/**
	 * Our constructor
	 */
	public ResponseGenerator() {
		
	}
	
	/**
	 * Date format that isn't being used.
	 */
	public static final SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
	
	/**
	 * Processes a request and fills in the ResponsePacket
	 * 
	 * @param request the request
	 * @param response the response to fill in
	 * @return returns the success of handling the request.
	 */
	public static boolean process(RequestPacket request, ResponsePacket response) {
		// Check if httpVersion is compatible
		if (!request.httpVersion.equals("HTTP/1.1")) {
			// NOTE: StatusCode.NEEDS_HTTP_1_1??
			if (request.httpVersion.equals("HTTP/1.0")) {
				request.headers.addHeader("Host", "");
			}
		}
		try {
			// Logger.log("rg");
			// response.headers.addHeader("Date", sdf.format(new Date())); timeless for optimization
			response.headers.addHeader("Server", "Avuna/" + AvunaHTTPD.VERSION);
			if (request.headers.hasHeader("Connection")) {
				response.headers.addHeader("Connection", request.headers.getHeader("Connection"));
			}
			if (request.host == null) {
				ResponseGenerator.generateDefaultResponse(response, StatusCode.INTERNAL_SERVER_ERROR);
				response.body = AvunaHTTPD.fileManager.getErrorPage(request, request.target, StatusCode.INTERNAL_SERVER_ERROR, "The requested host was not found on this server. Please contratc your server administrator.");
				return false;
			}
			EventMethodLookup elm = new EventMethodLookup(request.method, request, response);
			request.host.getHost().eventBus.callEvent(elm);
			if (!elm.isCanceled()) {
				generateDefaultResponse(response, StatusCode.NOT_YET_IMPLEMENTED);
				response.body = AvunaHTTPD.fileManager.getErrorPage(request, request.target, StatusCode.NOT_YET_IMPLEMENTED, "The requested URL " + request.target + " via " + request.method.name + " is not yet implemented.");
				return false;
			}else {
				// System.out.println((ah - start) / 1000000D + " start-ah");
				// System.out.println((ah2 - ah) / 1000000D + " ah-ah2");
				// System.out.println((ah3 - ah2) / 1000000D + " ah2-ah3");
				// System.out.println((cur - ah3) / 1000000D + " ah3-cur");
				return true;
			}
		}catch (Exception e) {
			Logger.logError(e);
			generateDefaultResponse(response, StatusCode.INTERNAL_SERVER_ERROR);
			response.body = AvunaHTTPD.fileManager.getErrorPage(request, request.target, StatusCode.INTERNAL_SERVER_ERROR, "The requested URL " + request.target + " caused a server failure.");
			return false;
		}
	}
	
	/**
	 * Generates the stausCode, httpVersion and reasonPhrase for the response
	 * 
	 * @param response the response packet
	 * @param status the status to set.
	 */
	public static void generateDefaultResponse(ResponsePacket response, StatusCode status) {
		response.statusCode = status.getStatus();
		response.httpVersion = "HTTP/1.1";
		response.reasonPhrase = status.getPhrase();
	}
	
}
