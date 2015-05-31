package org.avuna.httpd.http.plugins.base;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.http.ResponseGenerator;
import org.avuna.httpd.http.StatusCode;
import org.avuna.httpd.http.networking.Packet;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.Patch;
import org.avuna.httpd.http.plugins.PatchRegistry;
import org.avuna.httpd.http.util.CompiledDirective;
import org.avuna.httpd.http.util.OverrideConfig;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.Logger;

public class PatchOverride extends Patch {
	
	public PatchOverride(String name, PatchRegistry registry) {
		super(name, registry);
	}
	
	@Override
	public void formatConfig(ConfigNode json) {
		super.formatConfig(json);
	}
	
	@Override
	public boolean shouldProcessPacket(Packet packet) {
		return packet instanceof RequestPacket;
	}
	
	@Override
	public void processPacket(Packet packet) {
		RequestPacket request = (RequestPacket)packet;
		try {
			request.body = AvunaHTTPD.fileManager.preloadOverride(request, request.body, request.host.getHTDocs().getAbsolutePath());
		}catch (IOException e) {
			Logger.logError(e);
		}
		if (request.body == null || request.body.effectiveOverride == null) return;
		String rt = request.target;
		if (rt.contains("#")) rt = rt.substring(0, rt.indexOf("#"));
		String prt = "";
		if (rt.contains("?")) {
			prt = rt.substring(rt.indexOf("?"));
			rt = rt.substring(0, rt.indexOf("?"));
		}
		rt = request.host.getHostPath() + rt;
		if (rt.contains(".override")) {
			request.forbode = true;
			return;
		}
		rt += prt;
		OverrideConfig cfg = request.body.effectiveOverride;
		for (CompiledDirective d : cfg.getDirectives()) {
			switch (d.directive) {
			case forbid:
				if (rt.matches(d.args[0])) {
					request.forbode = true;
				}
				break;
			case redirect:
				request.oredir = rt.replaceAll(d.args[0], d.args[1]);
				break;
			case index:
				request.overrideIndex = d.args;
				break;
			case mime:
				if (rt.matches(d.args[1])) {
					request.overrideType = d.args[0];
					break;
				}
				break;
			case cache:
				if (rt.matches(d.args[1])) {
					request.overrideCache = d.args[0].equals("off") ? 0 : Integer.parseInt(d.args[0]);
					break;
				}
				break;
			case rewrite:
				request.rags1 = d.args[0];
				request.rags2 = d.args[1];
				break;
			}
		}
		return;
	}
	
	@Override
	public void processMethod(RequestPacket request, ResponsePacket response) {
		
	}
	
	public void reload() throws IOException {
		super.reload();
		nogo.clear();
		overrides.clear();
	}
	
	private ArrayList<String> nogo = new ArrayList<String>();
	private HashMap<String, HashMap<String, Object>> overrides = new HashMap<String, HashMap<String, Object>>();
	
	@Override
	public boolean shouldProcessResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		return request.forbode || request.oredir.length() > 0 || request.overrideType != null;
	}
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		if (request.forbode) {
			ResponseGenerator.generateDefaultResponse(response, StatusCode.FORBIDDEN);
			response.body = AvunaHTTPD.fileManager.getErrorPage(request, request.target, StatusCode.FORBIDDEN, "You don't have permission to access " + request.target + " on this server.");
			response.headers.updateHeader("Content-Type", response.body.type);
			return response.body.data;
		}else if (request.oredir.length() > 0) {
			ResponseGenerator.generateDefaultResponse(response, StatusCode.FOUND);
			response.headers.addHeader("Location", request.oredir);
			return null;
		}
		if (request.overrideType != null && response.body != null) {
			response.body.type = request.overrideType;
		}
		return data;
	}
	
}
