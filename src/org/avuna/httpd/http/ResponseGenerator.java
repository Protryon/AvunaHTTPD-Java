package org.avuna.httpd.http;

import java.text.SimpleDateFormat;
import org.avuna.httpd.AvunaHTTPD;
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
			long start = System.nanoTime();
			// response.headers.addHeader("Date", sdf.format(new Date())); timeless for optimization
			if (request.work.httpe) {
				response.headers.addHeader("X-Req-ID", request.headers.getHeader("X-Req-ID"));
				response.headers.addHeader("X-Req-Target", request.target);
			}
			long ah = System.nanoTime();
			response.headers.addHeader("Server", "Avuna/" + AvunaHTTPD.VERSION);
			long ah2 = System.nanoTime();
			if (request.headers.hasHeader("Connection")) {
				response.headers.addHeader("Connection", request.headers.getHeader("Connection"));
			}
			long ah3 = System.nanoTime();
			// Now we handle all of our patches such as php or jhtml which fill in the response.
			if (!request.host.getHost().patchBus.processMethod(request, response)) {
				generateDefaultResponse(response, StatusCode.NOT_YET_IMPLEMENTED);
				response.body = AvunaHTTPD.fileManager.getErrorPage(request, request.target, StatusCode.NOT_YET_IMPLEMENTED, "The requested URL " + request.target + " via " + request.method.name + " is not yet implemented.");
				return false;
			}else {
				long cur = System.nanoTime();
				// System.out.println((ah - start) / 1000000D + " start-ah");
				// System.out.println((ah2 - ah) / 1000000D + " ah-ah2");
				// System.out.println((ah3 - ah2) / 1000000D + " ah2-ah3");
				// System.out.println((cur - ah3) / 1000000D + " ah3-cur");
				return true;
			}
		}catch (Exception e) {
			Logger.logError(e);
			generateDefaultResponse(response, StatusCode.INTERNAL_SERVER_ERROR);
			response.body = AvunaHTTPD.fileManager.getErrorPage(request, request.target, StatusCode.NOT_YET_IMPLEMENTED, "The requested URL " + request.target + " caused a server failure.");
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
