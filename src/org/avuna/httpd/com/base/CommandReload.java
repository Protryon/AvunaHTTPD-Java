/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.com.base;

import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.com.Command;
import org.avuna.httpd.com.CommandContext;
import org.avuna.httpd.event.base.EventReload;
import org.avuna.httpd.hosts.Host;

public class CommandReload extends Command {
	
	@Override
	public int processCommand(String[] args, CommandContext context) throws Exception {
		AvunaHTTPD.mainConfig.load();
		AvunaHTTPD.hostsConfig.load();
		AvunaHTTPD.fileManager.clearCache();
		AvunaHTTPD.fileManager.flushjl();
		for (Host host : AvunaHTTPD.hosts.values()) {
			host.eventBus.callEvent(new EventReload());
		}
		context.println("Avuna Reloaded!");
		return 0;
	}
	
	@Override
	public String getHelp() {
		return "Reloads configuration files, and if a host is selected, reload that(and any VHosts).";
	}
}
