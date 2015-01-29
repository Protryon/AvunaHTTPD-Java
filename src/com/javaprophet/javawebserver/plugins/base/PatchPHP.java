package com.javaprophet.javawebserver.plugins.base;

/**
 * This class is deprecated, as we have proper CGI functionality now.
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Scanner;
import org.json.simple.JSONObject;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.http.Header;
import com.javaprophet.javawebserver.networking.Packet;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.Patch;

public class PatchPHP extends Patch {
	
	public PatchPHP(String name) {
		super(name);
	}
	
	@Override
	public boolean shouldProcessPacket(Packet packet) {
		return false;
	}
	
	@Override
	public void processPacket(Packet packet) {
		
	}
	
	@Override
	public boolean shouldProcessResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		return response.headers.hasHeader("Content-Type") && response.headers.getHeader("Content-Type").value.equals("application/x-php") && response.body != null && data != null;
	}
	
	private static final String crlf = System.getProperty("line.separator");
	
	@Override
	public void processMethod(RequestPacket request, ResponsePacket response) {
		
	}
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		try {
			String get = request.target;
			if (get.contains("#")) {
				get = get.substring(0, get.indexOf("#"));
			}
			String rq = get;
			if (get.contains("?")) {
				rq = get.substring(0, get.indexOf("?"));
				get = get.substring(get.indexOf("?") + 1);
			}else {
				get = "";
			}
			ProcessBuilder pb = new ProcessBuilder((String)pcfg.get("cmd"));
			pb.environment().put("CONTENT_LENGTH", request.body.getBody().data.length + "");
			pb.environment().put("CONTENT_TYPE", request.body.getBody().type);
			pb.environment().put("GATEWAY_INTERFACE", "CGI/1.1");
			pb.environment().put("PATH_INFO", request.target);
			pb.environment().put("PATH_TRANSLATED", request.target);
			pb.environment().put("QUERY_STRING", get);
			pb.environment().put("REMOTE_ADDR", request.userIP);
			pb.environment().put("REMOTE_HOST", request.userIP);
			pb.environment().put("REMOTE_PORT", request.userPort + "");
			pb.environment().put("REQUEST_METHOD", request.method.name);
			rq = JavaWebServer.fileManager.correctForIndex(rq);
			pb.environment().put("SCRIPT_NAME", rq.substring(rq.lastIndexOf("/") + 1));
			pb.environment().put("SERVER_NAME", request.headers.getHeader("Host").value);
			int port = 80;
			if (request.ssl) {
				port = ((Long)((JSONObject)JavaWebServer.mainConfig.get("ssl")).get("bindport")).intValue();
			}else {
				port = ((Long)JavaWebServer.mainConfig.get("bindport")).intValue();
			}
			pb.environment().put("SERVER_PORT", port + "");
			pb.environment().put("SERVER_PROTOCOL", request.httpVersion);
			pb.environment().put("SERVER_SOFTWARE", "JWS/" + JavaWebServer.VERSION);
			pb.environment().put("DOCUMENT_ROOT", JavaWebServer.fileManager.getHTDocs().getAbsolutePath().replace("\\", "/"));
			pb.environment().put("SCRIPT_FILENAME", JavaWebServer.fileManager.getAbsolutePath(rq).getAbsolutePath().replace("\\", "/"));
			pb.environment().put("REQUEST_URI", rq);
			for (Header header : request.headers.getHeaders()) {
				pb.environment().put("HTTP_" + header.name.toUpperCase().replace("-", "_"), header.value); // TODO: will break if multiple same-nameed headers are received
			}
			Process pbr = pb.start();
			OutputStream pbout = pbr.getOutputStream();
			if (request.body != null && request.body.getBody() != null) {
				pbout.write(request.body.getBody().data);
				pbout.flush();
			}
			Scanner s = new Scanner(pbr.getInputStream());
			boolean tt = true;
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			while (s.hasNextLine()) {
				String line = s.nextLine().trim();
				if (line.length() > 0) {
					if (tt && line.contains(":")) {
						String[] lt = line.split(":");
						String hn = lt[0].trim();
						String hd = lt[1].trim();
						if (hn.equals("Status")) {
							response.statusCode = Integer.parseInt(hd.substring(0, hd.indexOf(" ")));
							response.reasonPhrase = hd.substring(hd.indexOf(" ") + 1);
						}else {
							response.headers.updateHeader(hn, hd);
						}
					}else {
						tt = false;
						bout.write((line + crlf).getBytes());
					}
				}else {
					tt = false;
				}
			}
			if (response.headers.getHeader("Content-Type").value.equals("application/x-php")) {
				response.headers.updateHeader("Content-Type", "text/html");
			}
			s.close();
			return bout.toByteArray();
		}catch (IOException e) {
			e.printStackTrace(); // TODO: throws HTMLException?
		}
		return null;// TODO: to prevent PHP leaks
	}
	
	@Override
	public void formatConfig(JSONObject json) {
		if (!json.containsKey("cmd")) json.put("cmd", "php-cgi");
	}
	
}
