package org.avuna.httpd.http.plugins.base;

import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.event.Event;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.event.base.EventID;
import org.avuna.httpd.event.base.EventPreConnect;
import org.avuna.httpd.http.event.EventPreprocessRequest;
import org.avuna.httpd.http.event.HTTPEventID;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.plugins.Plugin;
import org.avuna.httpd.http.plugins.PluginRegistry;
import org.avuna.httpd.http.plugins.javaloader.JavaLoaderSecurity;
import org.avuna.httpd.http.plugins.javaloader.PluginJavaLoader;
import org.avuna.httpd.http.plugins.javaloader.security.JLSCompression;
import org.avuna.httpd.http.plugins.javaloader.security.JLSConnectionFlood;
import org.avuna.httpd.http.plugins.javaloader.security.JLSGetFlood;
import org.avuna.httpd.http.plugins.javaloader.security.JLSPardon;
import org.avuna.httpd.http.plugins.javaloader.security.JLSPostFlood;
import org.avuna.httpd.http.plugins.javaloader.security.JLSRequestFlood;
import org.avuna.httpd.http.plugins.javaloader.security.JLSUserAgent;
import org.avuna.httpd.util.ConfigNode;

public class PluginSecurity extends Plugin {
	
	public PluginSecurity(String name, PluginRegistry registry) {
		super(name, registry);
	}
	
	public void loadBases(PluginJavaLoader pjl) {
		pjl.loadBaseSecurity(new JLSPardon());
		pjl.loadBaseSecurity(new JLSConnectionFlood());
		pjl.loadBaseSecurity(new JLSPostFlood());
		pjl.loadBaseSecurity(new JLSCompression());
		pjl.loadBaseSecurity(new JLSRequestFlood());
		pjl.loadBaseSecurity(new JLSGetFlood());
		pjl.loadBaseSecurity(new JLSUserAgent());
	}
	
	private int minDrop = 100;
	
	@Override
	public void formatConfig(ConfigNode map) {
		super.formatConfig(map);
		if (!map.containsNode("minDrop")) map.insertNode("minDrop", "100");
		minDrop = Integer.parseInt(map.getNode("minDrop").getValue());
	}
	
	@Override
	public void receive(EventBus bus, Event event) {
		if (event instanceof EventPreprocessRequest) {
			EventPreprocessRequest epp = (EventPreprocessRequest)event;
			RequestPacket request = epp.getRequest();
			if (request.parent != null || PluginJavaLoader.security == null || PluginJavaLoader.security.size() < 1) return;
			int chance = 0;
			for (JavaLoaderSecurity sec : PluginJavaLoader.security) {
				chance += sec.check(request.userIP);
				chance += sec.check(request);
			}
			if (chance >= minDrop) {
				request.drop = true;
				AvunaHTTPD.bannedIPs.add(request.userIP);
			}
		}else if (event instanceof EventPreConnect) {
			// TODO:
		}
	}
	
	@Override
	public void register(EventBus bus) {
		bus.registerEvent(HTTPEventID.PREPROCESSREQUEST, this, 999);
		bus.registerEvent(EventID.PRECONNECT, this, 999);
	}
	
}
