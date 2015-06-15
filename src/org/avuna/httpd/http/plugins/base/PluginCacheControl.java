package org.avuna.httpd.http.plugins.base;

import org.avuna.httpd.event.Event;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.http.event.EventGenerateResponse;
import org.avuna.httpd.http.event.HTTPEventID;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.Plugin;
import org.avuna.httpd.http.plugins.PluginRegistry;
import org.avuna.httpd.util.ConfigNode;

public class PluginCacheControl extends Plugin {
	
	public PluginCacheControl(String name, PluginRegistry registry) {
		super(name, registry);
	}
	
	@Override
	public void formatConfig(ConfigNode json) {
		super.formatConfig(json);
		if (!json.containsNode("maxage")) json.insertNode("maxage", "604800");
		if (!json.containsNode("cache")) json.insertNode("cache", "text/css;application/javascript;image/*");
	}
	
	@Override
	public void receive(EventBus bus, Event event) {
		if (event instanceof EventGenerateResponse) {
			EventGenerateResponse egr = (EventGenerateResponse)event;
			ResponsePacket response = egr.getResponse();
			RequestPacket request = egr.getRequest();
			if (!(request.parent == null && response.body != null && response.headers.hasHeader("Content-Type"))) return;
			String ct = response.headers.getHeader("Content-Type");
			if (ct.contains(";")) ct = ct.substring(0, ct.indexOf(";")).trim();
			boolean nc = true;
			for (String s : ((String)pcfg.getNode("cache").getValue()).split(";")) {
				if (!s.endsWith("*") && s.equals(ct)) {
					nc = false;
					break;
				}else if (s.endsWith("*") && ct.startsWith(s.substring(0, s.length() - 1))) {
					nc = false;
					break;
				}
			}
			response.headers.addHeader("Cache-Control: max-age=" + Integer.parseInt((String)pcfg.getNode("maxage").getValue()) + (nc ? ", no-cache" : ""));
		}
	}
	
	@Override
	public void register(EventBus bus) {
		bus.registerEvent(HTTPEventID.GENERATERESPONSE, this, 900);
	}
}
