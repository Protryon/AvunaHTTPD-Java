package com.javaprophet.javawebserver.http;

import java.text.SimpleDateFormat;
import java.util.Date;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;

/**
 * Creates a http response coming from the server.
 */
public class ResponseGenerator {
	public ResponseGenerator() {
		
	}
	
	public static final SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
	
	public boolean process(RequestPacket request, ResponsePacket response) {
		if (!request.httpVersion.equals("HTTP/1.1")) {
			generateDefaultResponse(response, StatusCode.NEEDS_HTTP_1_1);
			return false;
		}
		try {
			// System.out.println("rg");
			long start = System.nanoTime();
			response.headers.addHeader("Date", sdf.format(new Date()));
			response.headers.addHeader("Server", "JWS/" + JavaWebServer.VERSION);
			if (request.headers.hasHeader("Connection")) {
				response.headers.addHeader("Connection", request.headers.getHeader("Connection").value);
			}
			long ah = System.nanoTime();
			if (!JavaWebServer.patchBus.processMethod(request, response)) {
				generateDefaultResponse(response, StatusCode.NOT_YET_IMPLEMENTED);
				JavaWebServer.fileManager.getErrorPage(response.body, request.target, StatusCode.NOT_YET_IMPLEMENTED, "The requested URL " + request.target + " via " + request.method.name + " is not yet implemented.");
				return false;
			}else {
				long cur = System.nanoTime();
				// System.out.println((ah - start) / 1000000D + " start-ah");
				// System.out.println((cur - ah) / 1000000D + " ah-cur");
				return true;
			}
		}catch (Exception e) {
			e.printStackTrace();
			generateDefaultResponse(response, StatusCode.INTERNAL_SERVER_ERROR);
			JavaWebServer.fileManager.getErrorPage(response.body, request.target, StatusCode.NOT_YET_IMPLEMENTED, "The requested URL " + request.target + " caused a server failure.");
			return false;
		}
	}
	
	public static void generateDefaultResponse(ResponsePacket response, StatusCode status) {
		response.statusCode = status.getStatus();
		response.httpVersion = "HTTP/1.1";
		response.reasonPhrase = status.getPhrase();
	}
	
}
