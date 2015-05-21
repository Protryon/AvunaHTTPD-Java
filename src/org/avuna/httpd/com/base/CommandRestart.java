package org.avuna.httpd.com.base;

import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.com.Command;
import org.avuna.httpd.com.CommandContext;
import org.avuna.httpd.util.CLib;

public class CommandRestart extends Command {
	
	@Override
	public int processCommand(String[] args, CommandContext context) throws Exception {
		if (AvunaHTTPD.windows) {
			Runtime.getRuntime().exec(AvunaHTTPD.fileManager.getBaseFile("restart.bat").getAbsolutePath());
		}else {
			if (CLib.getuid() != 0) {
				context.println("[CRITICAL] You must run as root to restart Avuna!");
				return 5; // TODO: if non-standard config, allow restart.
			}
			Runtime.getRuntime().exec("sh", new String[]{AvunaHTTPD.fileManager.getBaseFile("restart.sh").getAbsolutePath()});
		}
		context.println("Restarting...");
		return 0;
	}
	
	@Override
	public String getHelp() {
		return "Attempts to restart Avuna.";
	}
}
