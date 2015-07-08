/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins.security;

import java.io.IOException;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.event.Event;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.event.base.EventID;
import org.avuna.httpd.event.base.EventPreConnect;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.http.event.EventPreprocessRequest;
import org.avuna.httpd.http.event.HTTPEventID;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.plugins.Plugin;
import org.avuna.httpd.http.plugins.PluginRegistry;
import org.avuna.httpd.http.plugins.avunaagent.AvunaAgentSecurity;
import org.avuna.httpd.http.plugins.avunaagent.PluginAvunaAgent;
import org.avuna.httpd.http.plugins.avunaagent.security.JLSCompression;
import org.avuna.httpd.http.plugins.avunaagent.security.JLSConnectionFlood;
import org.avuna.httpd.http.plugins.avunaagent.security.JLSGetFlood;
import org.avuna.httpd.http.plugins.avunaagent.security.JLSPardon;
import org.avuna.httpd.http.plugins.avunaagent.security.JLSPostFlood;
import org.avuna.httpd.http.plugins.avunaagent.security.JLSRequestFlood;
import org.avuna.httpd.http.plugins.avunaagent.security.JLSUserAgent;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.Logger;

public class PluginSecurity extends Plugin {
	
	public PluginSecurity(String name, PluginRegistry registry) {
		super(name, registry);
	}
	
	public void loadBases(PluginAvunaAgent pjl) {
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
			EventPreprocessRequest epp = (EventPreprocessRequest) event;
			RequestPacket request = epp.getRequest();
			if (request.parent != null || PluginAvunaAgent.security == null || PluginAvunaAgent.security.size() < 1) return;
			int chance = 0;
			for (AvunaAgentSecurity sec : PluginAvunaAgent.security) {
				chance += sec.check(request.userIP);
				chance += sec.check(request);
			}
			if (chance >= minDrop) {
				request.drop = true;
				AvunaHTTPD.bannedIPs.add(request.userIP);
			}
		}else if (event instanceof EventPreConnect) {
			EventPreConnect epc = (EventPreConnect) event;
			int chance = 0;
			for (AvunaAgentSecurity sec : PluginAvunaAgent.security) {
				chance += sec.check(epc.getSocket().getInetAddress().getHostAddress());
			}
			if (chance >= minDrop) {
				try {
					epc.getSocket().close();
				}catch (IOException e) {
					Logger.logError(e);
				}
				AvunaHTTPD.bannedIPs.add(epc.getSocket().getInetAddress().getHostAddress());
				((HostHTTP) epc.getHost()).clearIPs(epc.getSocket().getInetAddress().getHostAddress()); // TODO: all host types
			}
		}
	}
	
	@Override
	public void register(EventBus bus) {
		bus.registerEvent(HTTPEventID.PREPROCESSREQUEST, this, 999);
		bus.registerEvent(EventID.PRECONNECT, this, 999);
	}
	
}
