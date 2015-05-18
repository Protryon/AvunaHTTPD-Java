package org.avuna.httpd.com.base;

import org.avuna.httpd.com.Command;
import org.avuna.httpd.com.CommandContext;

public class CommandMem extends Command {
	
	@Override
	public int processCommand(String[] args, CommandContext context) throws Exception {
		context.println(((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576) + "/" + (Runtime.getRuntime().totalMemory() / 1048576) + " (MB) memory used.");
		return 0;
	}
	
	@Override
	public String getHelp() {
		return "Returns memory statistics on Avuna's memory usage.";
	}
}
