package org.avuna.httpd.http.plugins.base;

import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.event.Event;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.http.Method;
import org.avuna.httpd.http.Resource;
import org.avuna.httpd.http.ResponseGenerator;
import org.avuna.httpd.http.StatusCode;
import org.avuna.httpd.http.event.EventMethodLookup;
import org.avuna.httpd.http.event.HTTPEventID;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.Patch;
import org.avuna.httpd.http.plugins.PatchRegistry;

public class PatchGetPostHead extends Patch {
	
	public PatchGetPostHead(String name, PatchRegistry registry) {
		super(name, registry);
	}
	
	@Override
	public void receive(EventBus bus, Event event) {
		if (event instanceof EventMethodLookup) {
			EventMethodLookup eml = ((EventMethodLookup)event);
			if (eml.getMethod().equals(Method.GET) || eml.getMethod().equals(Method.POST) || eml.getMethod().equals(Method.HEAD)) {
				RequestPacket request = eml.getRequest();
				ResponsePacket response = eml.getResponse();
				Resource resource = AvunaHTTPD.fileManager.getResource(request.target, request);
				if (resource == null) {
					ResponseGenerator.generateDefaultResponse(response, StatusCode.NOT_FOUND);
					response.body = AvunaHTTPD.fileManager.getErrorPage(request, request.target, StatusCode.NOT_FOUND, "The requested URL " + request.target + " was not found on this server.");
				}else {
					String rt = request.target;
					String get = "";
					if (rt.contains("#")) {
						rt = rt.substring(0, rt.indexOf("#"));
					}
					if (rt.contains("?")) {
						get = rt.substring(rt.indexOf("?"));
						rt = rt.substring(0, rt.indexOf("?"));
					}
					if (resource.wasDir && !rt.endsWith("/")) {
						ResponseGenerator.generateDefaultResponse(response, StatusCode.PERM_REDIRECT); // TODO: not relative
						response.headers.addHeader("Location", rt + "/" + get);
						response.headers.addHeader("Content-Length", "0");
						response.body = null;
					}else {
						ResponseGenerator.generateDefaultResponse(response, StatusCode.OK);
						response.body = resource;
					}
				}
				eml.cancel();
			}
		}
	}
	
	@Override
	public void register(EventBus bus) {
		bus.registerEvent(HTTPEventID.METHODLOOKUP, this, 900);
	}
}
