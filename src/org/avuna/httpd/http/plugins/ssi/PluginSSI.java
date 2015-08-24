/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins.ssi;

import java.io.File;
import org.avuna.httpd.event.Event;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.http.event.EventGenerateResponse;
import org.avuna.httpd.http.event.HTTPEventID;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.Plugin;
import org.avuna.httpd.http.plugins.PluginRegistry;
import org.avuna.httpd.http.plugins.ssi.directives.ConfigDirective;
import org.avuna.httpd.http.plugins.ssi.directives.EchoDirective;
import org.avuna.httpd.http.plugins.ssi.directives.ElifDirective;
import org.avuna.httpd.http.plugins.ssi.directives.ElseDirective;
import org.avuna.httpd.http.plugins.ssi.directives.EndIfDirective;
import org.avuna.httpd.http.plugins.ssi.directives.ExecDirective;
import org.avuna.httpd.http.plugins.ssi.directives.FlastmodDirective;
import org.avuna.httpd.http.plugins.ssi.directives.FsizeDirective;
import org.avuna.httpd.http.plugins.ssi.directives.IfDirective;
import org.avuna.httpd.http.plugins.ssi.directives.IncludeDirective;
import org.avuna.httpd.http.plugins.ssi.directives.PrintEnvDirective;
import org.avuna.httpd.http.plugins.ssi.directives.SetDirective;

public class PluginSSI extends Plugin {
	public final SSIEngine engine = new SSIEngine();
	
	public PluginSSI(String name, PluginRegistry registry, File config) {
		super(name, registry, config);
		engine.addDirective(new ConfigDirective(this));
		engine.addDirective(new EchoDirective(this));
		engine.addDirective(new ElifDirective(this));
		engine.addDirective(new ElseDirective(this));
		engine.addDirective(new EndIfDirective(this));
		engine.addDirective(new ExecDirective(this));
		engine.addDirective(new FlastmodDirective(this));
		engine.addDirective(new FsizeDirective(this));
		engine.addDirective(new IfDirective(this));
		engine.addDirective(new IncludeDirective(this));
		engine.addDirective(new PrintEnvDirective(this));
		engine.addDirective(new SetDirective(this));
	}
	
	@Override
	public void receive(EventBus bus, Event event) {
		if (event instanceof EventGenerateResponse) {
			EventGenerateResponse egr = (EventGenerateResponse) event;
			ResponsePacket response = egr.getResponse();
			RequestPacket request = egr.getRequest();
			if (!response.headers.hasHeader("Content-Type") || response.body == null || response.body.data == null) return;
			String ct = response.headers.getHeader("Content-Type");
			if (ct == null || !ct.startsWith("application/x-ssi")) return;
			response.headers.updateHeader("Content-Type", "text/html; charset=utf-8");
			if (response.body.data.length == 0) return;
			String body = new String(response.body.data);
			Page sp = engine.getParser().parsePage(body);
			StringBuilder res = new StringBuilder();
			int le = 0;
			if (sp != null && sp.directives != null) {
				sp.data = request;
				for (ParsedSSIDirective pd : sp.directives) {
					res.append(body.substring(le, pd.start));
					le = pd.end;
					String lr = engine.callDirective(sp, pd);
					if (lr == null) {
						response.body.data = sp.variables.get("error").getBytes();
						return;
					}else if (lr.length() > 0) res.append(lr);
				}
				sp.data = null;
				res.append(body.substring(le, body.length()));
				response.body.data = res.toString().getBytes();
			}
		}
	}
	
	@Override
	public void register(EventBus bus) {
		bus.registerEvent(HTTPEventID.GENERATERESPONSE, this, -700);
		bus.registerEvent(HTTPEventID.CLEARCACHE, this, 0);
	}
	
}
