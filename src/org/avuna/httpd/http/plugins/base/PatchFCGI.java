package org.avuna.httpd.http.plugins.base;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.event.Event;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.http.Resource;
import org.avuna.httpd.http.ResponseGenerator;
import org.avuna.httpd.http.StatusCode;
import org.avuna.httpd.http.event.EventGenerateResponse;
import org.avuna.httpd.http.event.EventReload;
import org.avuna.httpd.http.event.HTTPEventID;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.networking.Work;
import org.avuna.httpd.http.plugins.Patch;
import org.avuna.httpd.http.plugins.PatchRegistry;
import org.avuna.httpd.http.plugins.base.fcgi.FCGIConnection;
import org.avuna.httpd.http.plugins.base.fcgi.FCGIConnectionManagerNMPX;
import org.avuna.httpd.http.plugins.base.fcgi.FCGISession;
import org.avuna.httpd.http.plugins.base.fcgi.IFCGIManager;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.Logger;

public class PatchFCGI extends Patch {
	
	public PatchFCGI(String name, PatchRegistry registry) {
		super(name, registry);
		reloadus();
	}
	
	@Override
	public void receive(EventBus bus, Event event) {
		if (event instanceof EventGenerateResponse) {
			EventGenerateResponse egr = (EventGenerateResponse)event;
			ResponsePacket response = egr.getResponse();
			RequestPacket request = egr.getRequest();
			if (shouldProcessResponse(response, request)) processResponse(response, request);
		}else if (event instanceof EventReload) {
			reloadus();
		}
	}
	
	@Override
	public void register(EventBus bus) {
		bus.registerEvent(HTTPEventID.GENERATERESPONSE, this, -600);
		bus.registerEvent(HTTPEventID.RELOAD, this, 0);
	}
	
	public void reloadus() {
		if (!pcfg.getNode("enabled").getValue().equals("true")) return;
		for (IFCGIManager fcgi : fcgis.values()) {
			try {
				fcgi.close();
			}catch (IOException e) {
				Logger.logError(e);
			}
		}
		fcgis.clear();
		for (String subs : pcfg.getSubnodes()) {
			ConfigNode sub = pcfg.getNode(subs);
			if (!sub.branching()) continue;
			boolean unix = sub.getNode("unix").getValue().equals("true");
			try {
				String ip = sub.getNode("ip").getValue();
				int port = Integer.parseInt(sub.getNode("port").getValue());
				FCGIConnection sett = unix ? new FCGIConnection(ip) : new FCGIConnection(ip, port);
				sett.start();
				boolean cmpx = false;
				sett.getSettings();
				int i = 0;
				while (!sett.gotSettings) {
					i++;
					if (i > 10000) {
						break;
					}
					Thread.sleep(0L, 100000);
				}
				cmpx = sett.canMultiplex;
				sett.close();
				IFCGIManager mgr = null;
				if (cmpx) {
					mgr = unix ? new FCGIConnection(ip) : new FCGIConnection(ip, port);
				}else {
					mgr = unix ? new FCGIConnectionManagerNMPX(ip) : new FCGIConnectionManagerNMPX(ip, port);
				}
				fcgis.put(sub.getNode("mime-types").getValue(), mgr);
				mgr.start();
			}catch (Exception e) {
				fcgis.put(sub.getNode("mime-types").getValue(), null);
				Logger.logError(e);
				Logger.log("FCGI server(" + sub.getNode("ip").getValue() + (unix ? "" : ":" + sub.getNode("port").getValue()) + ") NOT accepting connections, disabling FCGI.");
			}
		}
	}
	
	private HashMap<String, IFCGIManager> fcgis = new HashMap<String, IFCGIManager>();
	
	@Override
	public void formatConfig(ConfigNode json) {
		if (!json.containsNode("enabled")) json.insertNode("enabled", "false");
		if (!json.containsNode("server_addr")) try {
			json.insertNode("server_addr", Inet4Address.getLocalHost().getHostAddress());
		}catch (UnknownHostException e) {
			json.insertNode("server_addr", "127.0.0.1");
		}
		if (!json.containsNode("php")) json.insertNode("php");
		for (String subb : json.getSubnodes()) {
			ConfigNode sub = json.getNode(subb);
			if (!sub.branching()) continue;
			if (!sub.containsNode("unix")) sub.insertNode("unix", "false", "set ip to the unix socket file, and port is ignored <to use unix sockets>");
			if (!sub.containsNode("ip")) sub.insertNode("ip", "127.0.0.1");
			if (!sub.containsNode("port")) sub.insertNode("port", "9000");
			if (!sub.containsNode("mime-types")) sub.insertNode("mime-types", "application/x-php");
		}
	}
	
	public boolean shouldProcessResponse(ResponsePacket response, RequestPacket request) {
		if (!response.headers.hasHeader("Content-Type") || response.body == null) return false;
		String ct = response.headers.getHeader("Content-Type");
		boolean gct = false;
		major:
		for (String key : fcgis.keySet()) {
			String[] pcts = key.split(",");
			for (String pct : pcts) {
				if (pct.trim().equals(ct)) {
					gct = true;
					break major;
				}
			}
		}
		return gct;
	}
	
	public void processResponse(ResponsePacket response, RequestPacket request) {
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
			FCGIConnection conn = null;
			String ct = response.headers.getHeader("Content-Type");
			major:
			for (String key : fcgis.keySet()) {
				String[] pcts = key.split(",");
				for (String pct : pcts) {
					if (pct.trim().equals(ct)) {
						IFCGIManager fcgi = fcgis.get(key);
						if (fcgi instanceof FCGIConnection) {
							conn = (FCGIConnection)fcgi;
						}else {
							FCGIConnectionManagerNMPX fcmx = (FCGIConnectionManagerNMPX)fcgi;
							if (fcmx == null) continue;
							conn = fcmx.getNMPX();
						}
						break major;
					}
				}
			}
			if (conn == null) {
				ResponseGenerator.generateDefaultResponse(response, StatusCode.INTERNAL_SERVER_ERROR);
				Resource er = AvunaHTTPD.fileManager.getErrorPage(request, request.target, StatusCode.INTERNAL_SERVER_ERROR, "Avuna encountered a critical error attempting to contact the FCGI Server! Please contact your system administrator and notify them to check their logs.");
				response.headers.updateHeader("Content-Type", er.type);
				response.body = er;
			}
			FCGISession session = new FCGISession(conn);
			session.start();
			session.param("SERVER_ADDR", pcfg.getNode("server_addr") + "");
			session.param("REQUEST_URI", rq + (get.length() > 0 ? "?" + get : ""));
			
			rq = AvunaHTTPD.fileManager.correctForIndex(rq, request);
			
			session.param("CONTENT_LENGTH", request.body.data.length + "");
			session.param("CONTENT_TYPE", request.body.type);
			session.param("GATEWAY_INTERFACE", "CGI/1.1");
			// session.param("PATH_INFO", request.extraPath);
			// session.param("PATH_TRANSLATED", new File(request.host.getHTDocs(), URLDecoder.decode(request.extraPath)).getAbsolutePath());
			session.param("QUERY_STRING", get);
			session.param("REMOTE_ADDR", request.userIP);
			session.param("REMOTE_HOST", request.userIP);
			session.param("REMOTE_PORT", request.userPort + "");
			session.param("REQUEST_METHOD", request.method.name);
			session.param("REDIRECT_STATUS", response.statusCode + "");
			String oabs = response.body.oabs.replace("\\", "/");
			String htds = request.host.getHTDocs().getAbsolutePath().replace("\\", "/");
			session.param("SCRIPT_NAME", oabs.substring(0, htds.length()));
			if (request.headers.hasHeader("Host")) session.param("SERVER_NAME", request.headers.getHeader("Host"));
			int port = request.host.getHost().getPort();
			session.param("SERVER_PORT", port + "");
			session.param("SERVER_PROTOCOL", request.httpVersion);
			session.param("SERVER_SOFTWARE", "Avuna/" + AvunaHTTPD.VERSION);
			session.param("DOCUMENT_ROOT", htds);
			session.param("SCRIPT_FILENAME", oabs);
			HashMap<String, ArrayList<String>> hdrs = request.headers.getHeaders();
			for (String key : hdrs.keySet()) {
				if (key.equalsIgnoreCase("Accept-Encoding")) continue;
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
			request.work.blockTimeout = true;
			try {
				int i = 0;
				while (!session.isDone()) {
					try {
						Work work = request.work;
						if (work == null && request.parent != null) {
							work = request.parent.work;
						}
						if (work != null && work.s.isClosed()) {
							session.abort();
							break;
						}
						i++;
						if (i > 600000) break;
						Thread.sleep(0L, 100000);
					}catch (InterruptedException e) {
						Logger.logError(e);
					}
				}
			}finally {
				request.work.blockTimeout = false;
			}
			Scanner s = new Scanner(new ByteArrayInputStream(session.getResponse()));
			boolean tt = true;
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			while (s.hasNextLine()) {
				String line = s.nextLine().trim();
				if (line.length() > 0) {
					if (tt && line.contains(":")) {
						String hn = line.substring(0, line.indexOf(":")).trim();
						if (hn.equalsIgnoreCase("Expires") || hn.equalsIgnoreCase("Last-Modified")) continue;
						String hd = line.substring(line.indexOf(":") + 1).trim();
						if (hn.equalsIgnoreCase("Status")) {
							response.statusCode = Integer.parseInt(hd.substring(0, hd.indexOf(" ")));
							response.reasonPhrase = hd.substring(hd.indexOf(" ") + 1);
						}else {
							if (hn.equals("Set-Cookie")) {
								response.headers.addHeader(hn, hd);
							}else {
								response.headers.updateHeader(hn, hd);
							}
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
			response.body.data = bout.toByteArray();
			return;
		}catch (IOException e) {
			Logger.logError(e); // TODO: throws HTMLException?
			ResponseGenerator.generateDefaultResponse(response, StatusCode.INTERNAL_SERVER_ERROR);
			Resource er = AvunaHTTPD.fileManager.getErrorPage(request, request.target, StatusCode.INTERNAL_SERVER_ERROR, "Avuna encountered a critical error attempting to contact the FCGI Server! Please contact your system administrator and notify them to check their logs.");
			response.headers.updateHeader("Content-Type", er.type);
			response.body = er;
		}
	}
}
