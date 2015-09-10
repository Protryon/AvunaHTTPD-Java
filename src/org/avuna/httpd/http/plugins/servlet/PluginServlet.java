package org.avuna.httpd.http.plugins.servlet;

import java.io.File;
import org.avuna.httpd.event.Event;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.http.event.HTTPEventID;
import org.avuna.httpd.http.plugins.Plugin;
import org.avuna.httpd.http.plugins.PluginRegistry;

public class PluginServlet extends Plugin {
	
	public PluginServlet(String name, PluginRegistry registry, File config) {
		super(name, registry, config);
	}
	
	@Override
	public void receive(EventBus bus, Event event) {
	
	}
	
	@Override
	public void register(EventBus bus) {
		bus.registerEvent(HTTPEventID.GENERATERESPONSE, this, -500);
	}
	
}
