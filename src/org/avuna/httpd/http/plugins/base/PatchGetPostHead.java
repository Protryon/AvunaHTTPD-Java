package org.avuna.httpd.http.plugins.base;

import java.util.HashMap;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.http.Method;
import org.avuna.httpd.http.Resource;
import org.avuna.httpd.http.ResponseGenerator;
import org.avuna.httpd.http.StatusCode;
import org.avuna.httpd.http.networking.Packet;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.Patch;
import org.avuna.httpd.http.plugins.PatchRegistry;

public class PatchGetPostHead extends Patch {
	
	public PatchGetPostHead(String name, PatchRegistry registry) {
		super(name, registry);
		registry.registerMethod(Method.GET, this);
		registry.registerMethod(Method.POST, this);
		registry.registerMethod(Method.HEAD, this);
	}
	
	@Override
	public void formatConfig(HashMap<String, Object> json) {
		super.formatConfig(json);
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
		return false;
	}
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		return data;
	}
	
	@Override
	public void processMethod(RequestPacket request, ResponsePacket response) {
		long start = System.nanoTime();
		Resource resource = AvunaHTTPD.fileManager.getResource(request.target, request);
		if (resource == null) {
			ResponseGenerator.generateDefaultResponse(response, StatusCode.NOT_FOUND);
			response.body = AvunaHTTPD.fileManager.getErrorPage(request, request.target, StatusCode.NOT_FOUND, "The requested URL " + request.target + " was not found on this server.");
			return;
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
			long rtd = System.nanoTime();
			if (resource.wasDir && !rt.endsWith("/")) {
				ResponseGenerator.generateDefaultResponse(response, StatusCode.PERM_REDIRECT); // TODO: not relative
				response.headers.addHeader("Location", rt + "/" + get);
				response.headers.addHeader("Content-Length", "0");
				response.body = null;
			}else {
				ResponseGenerator.generateDefaultResponse(response, StatusCode.OK);
				response.body = resource;
				// System.out.println((rtd - start) / 1000000D + " start-rtd");
				// System.out.println((System.nanoTime() - rtd) / 1000000D + " rtd-now");
			}
			return;
		}
	}
}
