package org.avuna.httpd.http.plugins.servlet;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import javax.servlet.Servlet;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.event.Event;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.http.Resource;
import org.avuna.httpd.http.ResponseGenerator;
import org.avuna.httpd.http.StatusCode;
import org.avuna.httpd.http.event.EventGenerateResponse;
import org.avuna.httpd.http.event.HTTPEventID;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.Plugin;
import org.avuna.httpd.http.plugins.PluginRegistry;
import org.avuna.httpd.util.ConfigNode;

public class PluginServlet extends Plugin {
	private static final class ServletData {
		public final String war;
		public final ServletClassLoader cl;
		
		public ServletData(String war, ServletClassLoader cl) {
			this.war = war;
			this.cl = cl;
		}
	}
	
	public PluginServlet(String name, PluginRegistry registry, File config) {
		super(name, registry, config);
		if (!pcfg.getNode("enabled").getValue().equals("true")) return;
		for (String subs : pcfg.getSubnodes()) {
			ConfigNode sub = pcfg.getNode(subs);
			if (!sub.branching()) continue;
			String dir = sub.getNode("mount-dir").getValue();
			String war = sub.getNode("war").getValue();
			try {
				servs.put(dir, new ServletData(war, loadWar(war)));
			}catch (IOException e) {
				registry.host.logger.logError(e);
			}
		}
	}
	
	private ServletClassLoader loadWar(String war) throws IOException {
		File absJar = null;
		if (AvunaHTTPD.windows && war.length() > 2 && war.charAt(1) == ':') {
			absJar = new File(war);
		}else if (!AvunaHTTPD.windows && war.startsWith("/")) {
			absJar = new File(war);
		}
		if (absJar == null) absJar = new File(AvunaHTTPD.fileManager.getMainDir(), war);
		return new ServletClassLoader(registry.host, new URL[] { absJar.toURI().toURL() }, this.getClass().getClassLoader(), absJar);
	}
	
	private HashMap<String, ServletData> servs = new HashMap<String, ServletData>();
	
	@Override
	public void formatConfig(ConfigNode json) {
		if (!json.containsNode("enabled")) json.insertNode("enabled", "false");
		if (!json.containsNode("server_addr")) try {
			json.insertNode("server_addr", Inet4Address.getLocalHost().getHostAddress());
		}catch (UnknownHostException e) {
			json.insertNode("server_addr", "127.0.0.1");
		}
		if (!json.containsNode("example")) json.insertNode("example");
		for (String subb : json.getSubnodes()) {
			ConfigNode sub = json.getNode(subb);
			if (!sub.branching()) continue;
			if (!sub.containsNode("war")) sub.insertNode("war", "myServlet.war", "relative to base avuna directory, or absolute. Can be a directory, or a WAR file.");
			if (!sub.containsNode("mount-dir")) sub.insertNode("mount-dir", "/myServlet", "directory in which to mount the servlet");
		}
	}
	
	@Override
	public void receive(EventBus bus, Event event) {
		if (event instanceof EventGenerateResponse) {
			EventGenerateResponse egr = (EventGenerateResponse) event;
			RequestPacket request = egr.getRequest();
			ResponsePacket response = egr.getResponse();
			ServletData war = null;
			String rp = null;
			for (String dir : servs.keySet()) {
				if (request.target.startsWith(dir)) {
					war = servs.get(dir);
					rp = request.target.substring(dir.length());
					if (!rp.startsWith("/")) rp = "/" + rp;
					break;
				}
			}
			if (war != null) {
				try {
					Servlet s = war.cl.getServlet(rp);
					if (s == null) {
						return;
					}
					request.procJL();
					ResponseGenerator.generateDefaultResponse(response, StatusCode.OK);
					AvunaServletResponse resp = new AvunaServletResponse(response, war.cl.context, s);
					s.service(new AvunaServletRequest(request, war.cl.context, s), resp);
					if (response.body == null) response.body = new Resource(resp.getOutput().getBytes(), response.headers.getHeader("Content-Type"));
					else response.body.data = resp.getOutput().getBytes();
				}catch (Exception e) {
					request.host.logger.logError(e);
					ResponseGenerator.generateDefaultResponse(response, StatusCode.INTERNAL_SERVER_ERROR);
					Resource rsc = AvunaHTTPD.fileManager.getErrorPage(request, request.target, StatusCode.INTERNAL_SERVER_ERROR, "Avuna had a critical error attempting to serve your page. Please contact your server administrator and try again. This error has been recorded in the Avuna log file.");
					response.headers.updateHeader("Content-Type", rsc.type);
					response.body = rsc;
					return;
				}
			}
		}
	}
	
	@Override
	public void register(EventBus bus) {
		bus.registerEvent(HTTPEventID.GENERATERESPONSE, this, -500);
	}
	
}
