package com.javaprophet.javawebserver.http;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.simple.JSONObject;
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
	
	public void process(RequestPacket request, ResponsePacket response) {
		if (!request.httpVersion.equals("HTTP/1.1")) {
			response.statusCode = 505;
			response.reasonPhrase = "NEEDS HTTP/1.1";
			response.httpVersion = request.httpVersion;
			return;
		}
		try {
			response.headers.addHeader("Date", sdf.format(new Date()));
			response.headers.addHeader("Server", "JWS/" + JavaWebServer.VERSION);
			if (request.headers.hasHeader("Connection")) {
				response.headers.addHeader("Connection", request.headers.getHeader("Connection").value);
			}
			if (request.method == Method.OPTIONS) {
				generateDefaultResponse(response, StatusCode.NOT_YET_IMPLEMENTED);
				getErrorPage(response.body, request.target, 501, "Not Yet Implemented", "The requested URL " + request.target + " via " + request.method.name + " is not yet implemented.");
				return;
			}else if (request.method == Method.GET || request.method == Method.HEAD || request.method == Method.POST) {
				Resource resource = getResource(request.target);
				if (resource == null || resource.data == null) {
					generateDefaultResponse(response, StatusCode.NOT_FOUND);
					getErrorPage(response.body, request.target, 404, "Not Found", "The requested URL " + request.target + " was not found on this server.");
					if (request.method == Method.HEAD) {
						response.headers.addHeader("Content-Length", response.body.getBody().data.length + "");
						response.body.setBody(null);
					}
					return;
				}else {
					generateDefaultResponse(response, StatusCode.OK);
					response.body.setBody(resource);
					if (request.method == Method.HEAD) {
						response.headers.addHeader("Content-Length", response.body.getBody().data.length + "");
						response.body.setBody(null);
					}
					return;
				}
			}else if (request.method == Method.PUT) {
				generateDefaultResponse(response, StatusCode.NOT_YET_IMPLEMENTED);
				getErrorPage(response.body, request.target, 501, "Not Yet Implemented", "The requested URL " + request.target + " via " + request.method.name + " is not yet implemented.");
				return;
			}else if (request.method == Method.DELETE) {
				generateDefaultResponse(response, StatusCode.NOT_YET_IMPLEMENTED);
				getErrorPage(response.body, request.target, 501, "Not Yet Implemented", "The requested URL " + request.target + " via " + request.method.name + " is not yet implemented.");
				return;
			}else if (request.method == Method.TRACE) {
				generateDefaultResponse(response, StatusCode.NOT_YET_IMPLEMENTED);
				getErrorPage(response.body, request.target, 501, "Not Yet Implemented", "The requested URL " + request.target + " via " + request.method.name + " is not yet implemented.");
				return;
			}else if (request.method == Method.CONNECT) {
				generateDefaultResponse(response, StatusCode.NOT_YET_IMPLEMENTED);
				getErrorPage(response.body, request.target, 501, "Not Yet Implemented", "The requested URL " + request.target + " via " + request.method.name + " is not yet implemented.");
				return;
			}
		}catch (Exception e) {
			e.printStackTrace();
			response.statusCode = 501;
			response.httpVersion = "HTTP/1.1";
			response.reasonPhrase = "Server Error";
			getErrorPage(response.body, request.target, 500, "Server Error", "The requested URL " + request.target + " caused a server failure.");
			return;
		}
	}

	public void generateDefaultResponse(ResponsePacket response, StatusCode status) {
		response.statusCode = status.getStatus();
		response.httpVersion = "HTTP/1.1";
		response.reasonPhrase = status.getPhrase();
	}
	
	public static final String crlf = System.getProperty("line.separator");
	
	public void getErrorPage(MessageBody body, String reqTarget, int statusCode, String reason, String info) {
		JSONObject errorPages = (JSONObject)JavaWebServer.mainConfig.get("errorpages");
		if (errorPages.containsKey(statusCode)) {
			try {
				String path = (String)errorPages.get(statusCode);
				Resource resource = getResource(path);
				if (resource != null) {
					if (resource.type.startsWith("text")) {
						String res = new String(resource.data);
						res = res.replace("$_statusCode_$", statusCode + "").replace("$_reason_$", reason).replace("$_info_$", info).replace("$_reqTarget_$", reqTarget);
						resource.data = res.getBytes();
					}
					body.setBody(resource);
					return;
				}
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		Resource error = new Resource(("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">" + crlf + "<html><head>" + crlf + "<title>" + statusCode + " " + reason + "</title>" + crlf + "</head><body>" + crlf + "<h1>" + reason + "</h1>" + crlf + "<p>" + info + "</p>" + crlf + "</body></html>").getBytes(), "text/html");
		body.setBody(error);
		return;
	}
	
	public File getAbsolutePath(String reqTarget) {
		File abs = new File(JavaWebServer.fileManager.getHTDocs(), reqTarget);
		if (abs.isDirectory()) {
			String[] index = ((String)JavaWebServer.mainConfig.get("index")).split(",");
			for (String i : index) {
				File f = new File(abs, i.trim());
				if (f.exists()) {
					abs = f;
					break;
				}
			}
		}
		return abs;
	}
	
	public Resource getResource(String reqTarget) {
		try {
			String rt = reqTarget;
			if (rt.contains("#")) {
				rt = rt.substring(0, rt.indexOf("#"));
			}
			if (rt.contains("?")) {
				rt = rt.substring(0, rt.indexOf("?"));
			}
			File abs = getAbsolutePath(rt);
			FileInputStream fin = new FileInputStream(abs);
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			int i = 1;
			byte[] buf = new byte[4096];
			while (i > 0) {
				i = fin.read(buf);
				if (i > 0) {
					bout.write(buf, 0, i);
				}
			}
			fin.close();
			byte[] resource = bout.toByteArray();
			String ext = abs.getName().substring(abs.getName().lastIndexOf(".") + 1);
			Resource r = new Resource(resource, JavaWebServer.extensionToMime.containsKey(ext) ? JavaWebServer.extensionToMime.get(ext) : "application/octet-stream", rt);
			return r;
		}catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
