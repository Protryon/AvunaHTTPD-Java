package org.avuna.httpd.com.base;

import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.com.Command;
import org.avuna.httpd.com.CommandContext;
import org.avuna.httpd.hosts.Host;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.http.plugins.base.PatchOverride;
import org.avuna.httpd.http.plugins.javaloader.PatchJavaLoader;

public class CommandReload extends Command {
	
	@Override
	public int processCommand(String[] args, CommandContext context) throws Exception {
		// Reloads our patches and config
		AvunaHTTPD.fileManager.clearCache();
		Host host = AvunaHTTPD.hosts.get(context.getSelectedHost());
		if (host != null && host instanceof HostHTTP) {
			((PatchOverride)((HostHTTP)host).registry.getPatchForClass(PatchOverride.class)).flush();
			((HostHTTP)host).patchBus.reload();
			((PatchJavaLoader)((HostHTTP)host).registry.getPatchForClass(PatchJavaLoader.class)).flushjl();
			AvunaHTTPD.fileManager.flushjl();
		}
		AvunaHTTPD.mainConfig.load();
		context.println("Avuna<" + context.getSelectedHost() + "> Reloaded!");
		return 0;
	}
	
	@Override
	public String getHelp() {
		return "Reloads configuration files, and if a host is selected, reload that(and any VHosts). This will not reload hosts.cfg.";
	}
}
