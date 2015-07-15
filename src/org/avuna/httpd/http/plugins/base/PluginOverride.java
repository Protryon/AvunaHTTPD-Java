/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins.base;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.event.Event;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.event.base.EventID;
import org.avuna.httpd.event.base.EventReload;
import org.avuna.httpd.http.ResponseGenerator;
import org.avuna.httpd.http.StatusCode;
import org.avuna.httpd.http.event.EventGenerateResponse;
import org.avuna.httpd.http.event.EventPreprocessRequest;
import org.avuna.httpd.http.event.HTTPEventID;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.Plugin;
import org.avuna.httpd.http.plugins.PluginRegistry;
import org.avuna.httpd.http.util.CompiledDirective;
import org.avuna.httpd.http.util.OverrideConfig;
import org.avuna.httpd.util.Logger;

public class PluginOverride extends Plugin {
	
	public PluginOverride(String name, PluginRegistry registry) {
		super(name, registry);
	}
	
	private ArrayList<String> nogo = new ArrayList<String>();
	private HashMap<String, HashMap<String, Object>> overrides = new HashMap<String, HashMap<String, Object>>();
	
	/** {@inheritDoc} */
	@Override
	public void receive(EventBus bus, Event event) {
		if (event instanceof EventGenerateResponse) {
			EventGenerateResponse egr = (EventGenerateResponse) event;
			ResponsePacket response = egr.getResponse();
			RequestPacket request = egr.getRequest();
			if (request.forbode) {
				ResponseGenerator.generateDefaultResponse(response, StatusCode.FORBIDDEN);
				response.body = AvunaHTTPD.fileManager.getErrorPage(request, request.target, StatusCode.FORBIDDEN, "You don't have permission to access " + request.target + " on this server.");
				response.headers.updateHeader("Content-Type", response.body.type);
				return;
			}else if (request.oredir.length() > 0) {
				ResponseGenerator.generateDefaultResponse(response, StatusCode.FOUND);
				response.headers.addHeader("Location", request.oredir);
				response.body = null;
				return;
			}
			if (request.overrideType != null && response.body != null) {
				response.body.type = request.overrideType;
			}
		}else if (event instanceof EventPreprocessRequest) {
			EventPreprocessRequest egr = (EventPreprocessRequest) event;
			RequestPacket request = egr.getRequest();
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
					case redirect:// TODO: what is this?
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
		}else if (event instanceof EventReload) {
			nogo.clear();
			overrides.clear();
		}
	}
	
	@Override
	public void register(EventBus bus) {
		bus.registerEvent(HTTPEventID.GENERATERESPONSE, this, 800);
		bus.registerEvent(HTTPEventID.PREPROCESSREQUEST, this, 800);
		bus.registerEvent(EventID.RELOAD, this, 0);
	}
	
}
