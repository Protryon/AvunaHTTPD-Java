package org.avuna.httpd.plugins.base;

/**
 * This class is deprecated, as we have proper CGI functionality now.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.http.networking.Packet;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.plugins.Patch;
import org.avuna.httpd.plugins.base.fcgi.FCGIConnection;
import org.avuna.httpd.plugins.base.fcgi.FCGISession;
import org.avuna.httpd.util.Logger;

public class PatchPHP extends Patch {
	private FCGIConnection conn;
	
	public PatchPHP(String name) {
		super(name);
		reload();
	}
	
	public void reload() {
		try {
			if (this.conn != null && !this.conn.isClosed()) {
				this.conn.close();
			}
			this.conn = new FCGIConnection((String)pcfg.get("ip"), Integer.parseInt((String)pcfg.get("port")));
			this.conn.start();
		}catch (IOException e) {
			Logger.logError(e);
			this.conn = null;
			Logger.log("FCGI server NOT accepting connections, disabling FCGI.");
		}
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
		return response.headers.hasHeader("Content-Type") && response.headers.getHeader("Content-Type").equals("application/x-php") && response.body != null && data != null;
	}
	
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
			// ProcessBuilder pb = new ProcessBuilder((String)pcfg.get("cmd"));
			FCGISession session = new FCGISession(conn);
			session.start();
			// pb.environment().start();
			session.param("REQUEST_URI", rq);
			
			rq = AvunaHTTPD.fileManager.correctForIndex(rq, request);
			
			session.param("CONTENT_LENGTH", request.body.data.length + "");
			session.param("CONTENT_TYPE", request.body.type);
			session.param("GATEWAY_INTERFACE", "CGI/1.1");
			// pb.environment().put("PATH_INFO", request.target);
			// pb.environment().put("PATH_TRANSLATED", new File(JavaWebServer.fileManager.getHTDocs(), rq).toString());
			session.param("QUERY_STRING", get);
			session.param("REMOTE_ADDR", request.userIP);
			session.param("REMOTE_HOST", request.userIP);
			session.param("REMOTE_PORT", request.userPort + "");
			session.param("REQUEST_METHOD", request.method.name);
			session.param("REDIRECT_STATUS", response.statusCode + "");
			session.param("SCRIPT_NAME", rq.substring(rq.lastIndexOf("/")));
			session.param("SERVER_NAME", request.headers.getHeader("Host"));
			int port = request.host.getHost().getPort();
			session.param("SERVER_PORT", port + "");
			session.param("SERVER_PROTOCOL", request.httpVersion);
			session.param("SERVER_SOFTWARE", "Avuna/" + AvunaHTTPD.VERSION);
			session.param("DOCUMENT_ROOT", request.host.getHTDocs().getAbsolutePath().replace("\\", "/"));
			session.param("SCRIPT_FILENAME", AvunaHTTPD.fileManager.getAbsolutePath(rq, request).getAbsolutePath().replace("\\", "/"));
			HashMap<String, ArrayList<String>> hdrs = request.headers.getHeaders();
			for (String key : hdrs.keySet()) {
				for (String val : hdrs.get(key)) {
					session.param("HTTP_" + key.toUpperCase().replace("-", "_"), val); // TODO: will break if multiple same-nameed headers are received
				}
			}
			session.finishParams();
			// Process pbr = pb.start();
			// while (!session.isDone()) {
			// try {
			// Thread.sleep(1L);
			// }catch (InterruptedException e) {
			// Logger.logError(e);
			// }
			// }
			// OutputStream pbout = ;
			if (request.body != null) {
				session.data(request.body.data);
				// pbout.write(request.body.data);
				// pbout.flush();
			}
			session.finishReq();
			while (!session.isDone()) {
				try {
					Thread.sleep(0L, 100000);
				}catch (InterruptedException e) {
					Logger.logError(e);
				}
			}
			Scanner s = new Scanner(new ByteArrayInputStream(session.getResponse()));
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
						bout.write((line + AvunaHTTPD.crlf).getBytes());
						if (line.equals("</html>")) break;
					}
				}else {
					tt = false;
				}
			}
			if (response.headers.getHeader("Content-Type").equals("application/x-php")) {
				response.headers.updateHeader("Content-Type", "text/html");
			}
			s.close();
			return bout.toByteArray();
		}catch (IOException e) {
			Logger.logError(e); // TODO: throws HTMLException?
		}
		return null;// TODO: to prevent PHP leaks
	}
	
	@Override
	public void formatConfig(HashMap<String, Object> json) {
		if (!json.containsKey("ip")) json.put("ip", "127.0.0.1");
		if (!json.containsKey("port")) json.put("port", "9000"); // TODO: hosts, contenttype association, etc
	}
	
}
